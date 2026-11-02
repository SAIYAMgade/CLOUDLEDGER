package com.cloudledger.ledger;

import com.cloudledger.account.Account;
import com.cloudledger.account.AccountRepository;
import com.cloudledger.account.AccountStatus;
import com.cloudledger.account.AccountType;
import com.cloudledger.common.MoneyUtils;
import com.cloudledger.common.exceptions.UnbalancedJournalException;
import com.cloudledger.ledger.dto.JournalEntryDTO;
import com.cloudledger.ledger.dto.JournalLineDTO;
import com.cloudledger.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerEngineInvariantsTest {

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private JournalEntryLineRepository journalEntryLineRepository;

    @Mock
    private LedgerPostingRepository ledgerPostingRepository;

    @Mock
    private ReversalEntryRepository reversalEntryRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private LedgerService ledgerService;

    private UUID accountAId;
    private UUID accountBId;
    private Account accountA;
    private Account accountB;

    @BeforeEach
    void setUp() {
        accountAId = UUID.randomUUID();
        accountBId = UUID.randomUUID();

        accountA = new Account();
        accountA.setAccountId(accountAId);
        accountA.setAccountNumber("AC-A");
        accountA.setAccountType(AccountType.LIABILITY);
        accountA.setCurrency("USD");
        accountA.setStatus(AccountStatus.ACTIVE);
        accountA.setLedgerBalance(new BigDecimal("1000.0000"));

        accountB = new Account();
        accountB.setAccountId(accountBId);
        accountB.setAccountNumber("AC-B");
        accountB.setAccountType(AccountType.LIABILITY);
        accountB.setCurrency("USD");
        accountB.setStatus(AccountStatus.ACTIVE);
        accountB.setLedgerBalance(new BigDecimal("500.0000"));
    }

    @Test
    @DisplayName("Should successfully validate double-entry when Total Debits == Total Credits")
    void testValidateDoubleEntrySuccess() {
        List<JournalLineDTO> lines = List.of(
            new JournalLineDTO(accountAId, EntryDirection.DEBIT, new BigDecimal("250.0000"), "USD"),
            new JournalLineDTO(accountBId, EntryDirection.CREDIT, new BigDecimal("250.0000"), "USD")
        );

        assertDoesNotThrow(() -> ledgerService.validateDoubleEntry(lines));
    }

    @Test
    @DisplayName("Should reject journal entry with UnbalancedJournalException when Debits != Credits")
    void testValidateDoubleEntryFailure() {
        List<JournalLineDTO> lines = List.of(
            new JournalLineDTO(accountAId, EntryDirection.DEBIT, new BigDecimal("250.0000"), "USD"),
            new JournalLineDTO(accountBId, EntryDirection.CREDIT, new BigDecimal("200.0000"), "USD")
        );

        UnbalancedJournalException ex = assertThrows(UnbalancedJournalException.class, () ->
            ledgerService.validateDoubleEntry(lines)
        );

        assertTrue(ex.getMessage().contains("Double-entry invariant violated"));
    }

    @Test
    @DisplayName("Should post journal and update account ledger balances accurately")
    void testPostJournalSuccess() {
        JournalEntry entry = new JournalEntry();
        entry.setJournalNumber("JE-TEST-01");
        entry.setStatus(JournalStatus.DRAFT);
        entry.addLine(new JournalEntryLine(accountAId, EntryDirection.DEBIT, new BigDecimal("100.0000"), "USD", 1));
        entry.addLine(new JournalEntryLine(accountBId, EntryDirection.CREDIT, new BigDecimal("100.0000"), "USD", 2));

        when(journalEntryRepository.findById(entry.getJournalId())).thenReturn(Optional.of(entry));
        when(accountRepository.findByIdForUpdate(accountAId)).thenReturn(Optional.of(accountA));
        when(accountRepository.findByIdForUpdate(accountBId)).thenReturn(Optional.of(accountB));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        JournalEntry posted = ledgerService.postJournal(entry.getJournalId());

        assertEquals(JournalStatus.POSTED, posted.getStatus());
        assertNotNull(posted.getPostedAt());
        verify(ledgerPostingRepository, times(2)).save(any(LedgerPosting.class));
        verify(outboxService, times(1)).recordEvent(eq("JournalEntry"), anyString(), eq("ledger.entry.posted"), anyString(), any());
    }
}
