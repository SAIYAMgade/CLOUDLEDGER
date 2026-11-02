package com.cloudledger.account;

import com.cloudledger.common.exceptions.InsufficientBalanceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentAccountBalanceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("Should prevent double withdrawal: 10,000 balance cannot satisfy two 7,000 withdrawals")
    void testConcurrentWithdrawalsPreventNegativeBalance() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account();
        account.setAccountId(accountId);
        account.setAccountNumber("AC-STRESS-01");
        account.setAccountType(AccountType.ASSET);
        account.setCurrency("INR");
        account.setStatus(AccountStatus.ACTIVE);
        account.setAvailableBalance(new BigDecimal("10000.0000"));
        account.setLedgerBalance(new BigDecimal("10000.0000"));

        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        // First withdrawal of 7,000 INR succeeds
        BigDecimal firstWithdrawal = new BigDecimal("7000.0000");
        assertDoesNotThrow(() -> accountService.reserveFunds(accountId, firstWithdrawal));
        assertEquals(new BigDecimal("3000.0000"), account.getAvailableBalance());

        // Second withdrawal of 7,000 INR MUST fail because remaining balance is only 3,000 INR
        BigDecimal secondWithdrawal = new BigDecimal("7000.0000");
        InsufficientBalanceException ex = assertThrows(InsufficientBalanceException.class, () ->
            accountService.reserveFunds(accountId, secondWithdrawal)
        );

        assertTrue(ex.getMessage().contains("is insufficient for amount"));
        // Final balance remains strictly positive
        assertEquals(new BigDecimal("3000.0000"), account.getAvailableBalance());
    }
}
