package com.cloudledger.ledger;

import com.cloudledger.account.Account;
import com.cloudledger.account.AccountRepository;
import com.cloudledger.account.AccountStatus;
import com.cloudledger.common.MoneyUtils;
import com.cloudledger.common.exceptions.AccountFrozenException;
import com.cloudledger.common.exceptions.BusinessException;
import com.cloudledger.common.exceptions.ResourceNotFoundException;
import com.cloudledger.common.exceptions.UnbalancedJournalException;
import com.cloudledger.ledger.dto.GeneralLedgerDTO;
import com.cloudledger.ledger.dto.JournalEntryDTO;
import com.cloudledger.ledger.dto.JournalLineDTO;
import com.cloudledger.ledger.dto.TrialBalanceDTO;
import com.cloudledger.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final LedgerPostingRepository ledgerPostingRepository;
    private final ReversalEntryRepository reversalEntryRepository;
    private final AccountRepository accountRepository;
    private final OutboxService outboxService;

    public LedgerService(JournalEntryRepository journalEntryRepository,
                         JournalEntryLineRepository journalEntryLineRepository,
                         LedgerPostingRepository ledgerPostingRepository,
                         ReversalEntryRepository reversalEntryRepository,
                         AccountRepository accountRepository,
                         OutboxService outboxService) {
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
        this.ledgerPostingRepository = ledgerPostingRepository;
        this.reversalEntryRepository = reversalEntryRepository;
        this.accountRepository = accountRepository;
        this.outboxService = outboxService;
    }

    /**
     * Validates that total debits strictly equal total credits (Debit == Credit).
     */
    public void validateDoubleEntry(List<JournalLineDTO> lines) {
        if (lines == null || lines.size() < 2) {
            throw new UnbalancedJournalException("A journal entry requires at least two lines");
        }

        BigDecimal totalDebits = MoneyUtils.ZERO;
        BigDecimal totalCredits = MoneyUtils.ZERO;
        String baseCurrency = null;

        for (JournalLineDTO line : lines) {
            if (line.getAmount() == null || line.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("INVALID_LINE_AMOUNT", "Line amount must be positive");
            }
            if (baseCurrency == null) {
                baseCurrency = line.getCurrency();
            } else if (!baseCurrency.equalsIgnoreCase(line.getCurrency())) {
                throw new BusinessException("CURRENCY_MISMATCH", "Multi-currency journal lines require FX clearance entries");
            }

            if (line.getDirection() == EntryDirection.DEBIT) {
                totalDebits = totalDebits.add(line.getAmount());
            } else if (line.getDirection() == EntryDirection.CREDIT) {
                totalCredits = totalCredits.add(line.getAmount());
            }
        }

        totalDebits = MoneyUtils.format(totalDebits);
        totalCredits = MoneyUtils.format(totalCredits);

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new UnbalancedJournalException(
                String.format("Double-entry invariant violated: Total Debits (%s) != Total Credits (%s)", totalDebits, totalCredits)
            );
        }
    }

    @Transactional
    public JournalEntry createJournalEntry(JournalEntryDTO req) {
        validateDoubleEntry(req.getLines());

        String journalNumber = req.getJournalNumber() != null ? req.getJournalNumber() :
                "JE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        JournalEntry entry = new JournalEntry();
        entry.setJournalNumber(journalNumber);
        entry.setTransactionId(req.getTransactionId());
        entry.setDescription(req.getDescription());
        entry.setStatus(JournalStatus.DRAFT);
        entry.setCreatedBy(req.getCreatedBy() != null ? req.getCreatedBy() : "SYSTEM");

        int order = 1;
        for (JournalLineDTO lineDTO : req.getLines()) {
            JournalEntryLine line = new JournalEntryLine(
                lineDTO.getAccountId(),
                lineDTO.getDirection(),
                lineDTO.getAmount(),
                lineDTO.getCurrency().toUpperCase(),
                order++
            );
            entry.addLine(line);
        }

        return journalEntryRepository.save(entry);
    }

    /**
     * Authoritative Posting: Applies ledger postings and locks accounts in deterministic order.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public JournalEntry postJournal(UUID journalId) {
        JournalEntry journal = journalEntryRepository.findById(journalId)
                .orElseThrow(() -> new ResourceNotFoundException("Journal entry not found: " + journalId));

        if (journal.getStatus() != JournalStatus.DRAFT) {
            throw new BusinessException("INVALID_JOURNAL_STATE", "Only DRAFT journals can be posted. Current status: " + journal.getStatus());
        }

        // Sort accounts by ID ascending to avoid deadlocks during concurrent multi-account postings
        List<UUID> accountIds = journal.getLines().stream()
                .map(JournalEntryLine::getAccountId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        Map<UUID, Account> lockedAccounts = new HashMap<>();
        for (UUID accId : accountIds) {
            Account acc = accountRepository.findByIdForUpdate(accId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accId));
            if (acc.getStatus() != AccountStatus.ACTIVE) {
                throw new AccountFrozenException("Account " + acc.getAccountNumber() + " is " + acc.getStatus());
            }
            lockedAccounts.put(accId, acc);
        }

        // Execute postings
        for (JournalEntryLine line : journal.getLines()) {
            Account acc = lockedAccounts.get(line.getAccountId());
            BigDecimal amount = line.getAmount();

            // Calculate running ledger balance
            BigDecimal newLedgerBalance;
            if (acc.getAccountType().isNormalDebit()) {
                newLedgerBalance = (line.getDirection() == EntryDirection.DEBIT) ?
                        acc.getLedgerBalance().add(amount) : acc.getLedgerBalance().subtract(amount);
            } else {
                newLedgerBalance = (line.getDirection() == EntryDirection.CREDIT) ?
                        acc.getLedgerBalance().add(amount) : acc.getLedgerBalance().subtract(amount);
            }

            acc.setLedgerBalance(newLedgerBalance);
            accountRepository.save(acc);

            LedgerPosting posting = new LedgerPosting(
                journal.getJournalId(),
                acc.getAccountId(),
                line.getDirection(),
                amount,
                line.getCurrency(),
                newLedgerBalance
            );
            ledgerPostingRepository.save(posting);
        }

        journal.setStatus(JournalStatus.POSTED);
        journal.setPostedAt(Instant.now());
        JournalEntry saved = journalEntryRepository.save(journal);

        // Record outbox event atomically
        outboxService.recordEvent(
            "JournalEntry",
            saved.getJournalId().toString(),
            "ledger.entry.posted",
            saved.getJournalNumber(),
            Map.of("journalId", saved.getJournalId(), "journalNumber", saved.getJournalNumber(), "status", "POSTED")
        );

        log.info("Posted Journal Entry: number={}, lines={}", saved.getJournalNumber(), saved.getLines().size());
        return saved;
    }

    /**
     * Auditable Reversal: Generates an inverse journal entry and links it via reversal_entries.
     */
    @Transactional
    public JournalEntry reverseJournal(UUID originalJournalId, String reason, String reversedBy) {
        JournalEntry original = journalEntryRepository.findById(originalJournalId)
                .orElseThrow(() -> new ResourceNotFoundException("Original journal not found: " + originalJournalId));

        if (original.getStatus() != JournalStatus.POSTED) {
            throw new BusinessException("CANNOT_REVERSE_UNPOSTED", "Only POSTED journals can be reversed");
        }

        if (reversalEntryRepository.findByOriginalJournalId(originalJournalId).isPresent()) {
            throw new BusinessException("ALREADY_REVERSED", "Journal entry has already been reversed");
        }

        // Construct inverse lines (Debit <-> Credit swap)
        List<JournalLineDTO> inverseLines = original.getLines().stream().map(line -> {
            EntryDirection inverseDir = (line.getDirection() == EntryDirection.DEBIT) ?
                    EntryDirection.CREDIT : EntryDirection.DEBIT;
            return new JournalLineDTO(line.getAccountId(), inverseDir, line.getAmount(), line.getCurrency());
        }).collect(Collectors.toList());

        JournalEntryDTO reversalDTO = new JournalEntryDTO();
        reversalDTO.setDescription("Reversal of " + original.getJournalNumber() + ": " + reason);
        reversalDTO.setJournalNumber("REV-" + original.getJournalNumber());
        reversalDTO.setLines(inverseLines);
        reversalDTO.setCreatedBy(reversedBy);

        JournalEntry reversalJournal = createJournalEntry(reversalDTO);
        JournalEntry postedReversal = postJournal(reversalJournal.getJournalId());

        original.setStatus(JournalStatus.REVERSED);
        journalEntryRepository.save(original);

        ReversalEntry record = new ReversalEntry(original.getJournalId(), postedReversal.getJournalId(), reason, reversedBy);
        reversalEntryRepository.save(record);

        log.warn("Reversed journal {}: reversal journal is {}", original.getJournalNumber(), postedReversal.getJournalNumber());
        return postedReversal;
    }

    @Transactional(readOnly = true)
    public TrialBalanceDTO generateTrialBalance() {
        List<Account> accounts = accountRepository.findAll();
        List<TrialBalanceDTO.TrialBalanceItem> items = new ArrayList<>();

        BigDecimal totalDebits = MoneyUtils.ZERO;
        BigDecimal totalCredits = MoneyUtils.ZERO;

        for (Account a : accounts) {
            BigDecimal bal = a.getLedgerBalance();
            BigDecimal debitAmount = MoneyUtils.ZERO;
            BigDecimal creditAmount = MoneyUtils.ZERO;

            if (a.getAccountType().isNormalDebit()) {
                if (bal.compareTo(BigDecimal.ZERO) >= 0) {
                    debitAmount = bal;
                } else {
                    creditAmount = bal.abs();
                }
            } else {
                if (bal.compareTo(BigDecimal.ZERO) >= 0) {
                    creditAmount = bal;
                } else {
                    debitAmount = bal.abs();
                }
            }

            totalDebits = totalDebits.add(debitAmount);
            totalCredits = totalCredits.add(creditAmount);

            items.add(new TrialBalanceDTO.TrialBalanceItem(
                a.getAccountId(),
                a.getAccountNumber(),
                a.getAccountName(),
                a.getAccountType().name(),
                debitAmount,
                creditAmount
            ));
        }

        totalDebits = MoneyUtils.format(totalDebits);
        totalCredits = MoneyUtils.format(totalCredits);

        TrialBalanceDTO tb = new TrialBalanceDTO();
        tb.setAsOfDate(Instant.now());
        tb.setCurrency("USD");
        tb.setTotalDebits(totalDebits);
        tb.setTotalCredits(totalCredits);
        tb.setBalanced(totalDebits.compareTo(totalCredits) == 0);
        tb.setItems(items);
        return tb;
    }

    @Transactional(readOnly = true)
    public GeneralLedgerDTO getGeneralLedger(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        List<LedgerPosting> postings = ledgerPostingRepository.findByAccountIdOrderByPostedAtAsc(accountId);
        List<GeneralLedgerDTO.LedgerRecordDTO> records = postings.stream()
                .map(p -> new GeneralLedgerDTO.LedgerRecordDTO(
                    p.getPostingId(),
                    p.getJournalId(),
                    p.getPostedAt(),
                    p.getDirection().name(),
                    p.getAmount(),
                    p.getRunningBalance()
                )).collect(Collectors.toList());

        GeneralLedgerDTO gl = new GeneralLedgerDTO();
        gl.setAccountId(account.getAccountId());
        gl.setAccountNumber(account.getAccountNumber());
        gl.setAccountName(account.getAccountName());
        gl.setAccountType(account.getAccountType().name());
        gl.setCurrency(account.getCurrency());
        gl.setCurrentBalance(account.getLedgerBalance());
        gl.setRecords(records);
        return gl;
    }
}
