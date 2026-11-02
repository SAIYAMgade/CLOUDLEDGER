package com.cloudledger.account;

import com.cloudledger.account.dto.AccountResponse;
import com.cloudledger.account.dto.CreateAccountRequest;
import com.cloudledger.common.MoneyUtils;
import com.cloudledger.common.exceptions.AccountFrozenException;
import com.cloudledger.common.exceptions.InsufficientBalanceException;
import com.cloudledger.common.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest req) {
        String generatedAccountNumber = "AC-" + req.getCurrency().toUpperCase() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Account account = new Account();
        account.setAccountNumber(generatedAccountNumber);
        account.setAccountName(req.getAccountName());
        account.setCustomerReference(req.getCustomerReference());
        account.setAccountType(req.getAccountType());
        account.setCurrency(req.getCurrency().toUpperCase());
        account.setStatus(AccountStatus.ACTIVE);
        account.setAvailableBalance(MoneyUtils.ZERO);
        account.setLedgerBalance(MoneyUtils.ZERO);

        Account saved = accountRepository.save(account);
        log.info("Created account: id={}, number={}, type={}", saved.getAccountId(), saved.getAccountNumber(), saved.getAccountType());
        return AccountResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public Account getAccountEntity(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listAccounts() {
        return accountRepository.findAll().stream()
                .map(AccountResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountResponse freezeAccount(UUID accountId, String reason) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
        account.setStatus(AccountStatus.FROZEN);
        account.setUpdatedAt(Instant.now());
        Account saved = accountRepository.save(account);
        log.warn("Account frozen: id={}, reason={}", accountId, reason);
        return AccountResponse.from(saved);
    }

    @Transactional
    public AccountResponse unfreezeAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
        account.setStatus(AccountStatus.ACTIVE);
        account.setUpdatedAt(Instant.now());
        Account saved = accountRepository.save(account);
        log.info("Account unfrozen: id={}", accountId);
        return AccountResponse.from(saved);
    }

    /**
     * Reserve funds on the source account under pessimistic row lock.
     * Prevents race conditions during concurrent withdrawal attempts.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void reserveFunds(UUID accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        if (!account.getStatus().canPost()) {
            throw new AccountFrozenException("Account is not active: status is " + account.getStatus());
        }

        // For asset or expense accounts, enforce available balance sufficiency
        if (account.getAccountType().isNormalDebit() || account.getAccountType() == AccountType.LIABILITY) {
            if (account.getAvailableBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException("Available balance " + account.getAvailableBalance() + " is insufficient for amount " + amount);
            }
        }

        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);
        log.debug("Reserved {} on account {}, new available balance: {}", amount, accountId, account.getAvailableBalance());
    }

    /**
     * Release previously reserved funds if transfer saga fails.
     */
    @Transactional
    public void releaseReservedFunds(UUID accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
        account.setAvailableBalance(account.getAvailableBalance().add(amount));
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);
        log.info("Released reserved {} back to account {}", amount, accountId);
    }

    /**
     * Commit funds to the destination account (credits available and ledger balance).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void commitCredit(UUID accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        if (!account.getStatus().canPost()) {
            throw new AccountFrozenException("Destination account is not active: status is " + account.getStatus());
        }

        account.setAvailableBalance(account.getAvailableBalance().add(amount));
        account.setLedgerBalance(account.getLedgerBalance().add(amount));
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);
        log.debug("Credited {} to account {}", amount, accountId);
    }

    /**
     * Commit previously reserved funds on source account by decrementing ledger balance.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void commitDebit(UUID accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        account.setLedgerBalance(account.getLedgerBalance().subtract(amount));
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);
        log.debug("Debited ledger balance by {} on account {}", amount, accountId);
    }
}
