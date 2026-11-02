package com.cloudledger.generator;

import com.cloudledger.account.Account;
import com.cloudledger.account.AccountRepository;
import com.cloudledger.account.AccountStatus;
import com.cloudledger.account.AccountType;
import com.cloudledger.common.MoneyUtils;
import com.cloudledger.transaction.Transaction;
import com.cloudledger.transaction.TransactionRepository;
import com.cloudledger.transaction.TransactionStatus;
import com.cloudledger.transaction.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class SyntheticDataGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(SyntheticDataGeneratorService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public SyntheticDataGeneratorService(AccountRepository accountRepository,
                                         TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Seeds synthetic accounts and transactions using a deterministic random seed.
     */
    @Transactional
    public Map<String, Object> generateData(int accountCount, int transactionCount, long seed) {
        Random random = new Random(seed);
        log.info("Generating synthetic dataset: accounts={}, transactions={}, seed={}", accountCount, transactionCount, seed);

        List<Account> createdAccounts = new ArrayList<>();
        String[] currencies = {"USD", "EUR", "GBP"};
        AccountType[] types = {AccountType.LIABILITY, AccountType.ASSET, AccountType.EXPENSE, AccountType.REVENUE};

        for (int i = 1; i <= accountCount; i++) {
            String curr = currencies[random.nextInt(currencies.length)];
            AccountType type = types[random.nextInt(types.length)];
            BigDecimal initialBal = BigDecimal.valueOf(random.nextInt(50000) + 1000).setScale(4);

            Account account = new Account();
            account.setAccountNumber("AC-SYN-" + curr + "-" + String.format("%05d", i));
            account.setAccountName("Synthetic Account #" + i);
            account.setCustomerReference("CUST-SYN-" + String.format("%04d", (i % 500) + 1));
            account.setAccountType(type);
            account.setCurrency(curr);
            account.setStatus(AccountStatus.ACTIVE);
            account.setAvailableBalance(initialBal);
            account.setLedgerBalance(initialBal);
            createdAccounts.add(account);
        }

        createdAccounts = accountRepository.saveAll(createdAccounts);

        List<Transaction> createdTransactions = new ArrayList<>();
        TransactionStatus[] statuses = {
            TransactionStatus.POSTED, TransactionStatus.POSTED, TransactionStatus.POSTED,
            TransactionStatus.POSTED, TransactionStatus.FAILED, TransactionStatus.REVERSED
        };

        for (int j = 1; j <= transactionCount; j++) {
            Account src = createdAccounts.get(random.nextInt(createdAccounts.size()));
            Account dest = createdAccounts.get(random.nextInt(createdAccounts.size()));
            while (src.getAccountId().equals(dest.getAccountId())) {
                dest = createdAccounts.get(random.nextInt(createdAccounts.size()));
            }

            BigDecimal amount = BigDecimal.valueOf(random.nextInt(2000) + 10).setScale(4);
            TransactionStatus status = statuses[random.nextInt(statuses.length)];
            Instant createdAt = Instant.now().minus(random.nextInt(30), ChronoUnit.DAYS);

            Transaction tx = new Transaction();
            tx.setExternalReference("TX-SYN-" + seed + "-" + String.format("%06d", j));
            tx.setSourceAccountId(src.getAccountId());
            tx.setDestinationAccountId(dest.getAccountId());
            tx.setAmount(amount);
            tx.setCurrency(src.getCurrency());
            tx.setTransactionType(TransactionType.TRANSFER);
            tx.setStatus(status);
            tx.setIdempotencyKey("IDEMP-SYN-" + seed + "-" + j);
            tx.setDescription("Synthetic transaction transfer #" + j);
            tx.setCreatedAt(createdAt);
            tx.setUpdatedAt(createdAt);

            if (status == TransactionStatus.POSTED) {
                tx.setCompletedAt(createdAt.plus(200, ChronoUnit.MILLIS));
            } else if (status == TransactionStatus.FAILED) {
                tx.setErrorCode("INSUFFICIENT_FUNDS");
                tx.setErrorMessage("Available balance is insufficient for simulated transaction");
                tx.setCompletedAt(createdAt.plus(50, ChronoUnit.MILLIS));
            } else if (status == TransactionStatus.REVERSED) {
                tx.setCompletedAt(createdAt.plus(1, ChronoUnit.HOURS));
            }

            createdTransactions.add(tx);
        }

        createdTransactions = transactionRepository.saveAll(createdTransactions);

        log.info("Successfully generated {} accounts and {} transactions", createdAccounts.size(), createdTransactions.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accountsGenerated", createdAccounts.size());
        result.put("transactionsGenerated", createdTransactions.size());
        result.put("seedUsed", seed);
        result.put("status", "COMPLETED");
        return result;
    }
}
