package com.cloudledger.saga;

import com.cloudledger.account.Account;
import com.cloudledger.account.AccountRepository;
import com.cloudledger.account.AccountService;
import com.cloudledger.account.AccountStatus;
import com.cloudledger.account.AccountType;
import com.cloudledger.common.exceptions.InsufficientBalanceException;
import com.cloudledger.ledger.JournalEntry;
import com.cloudledger.ledger.JournalStatus;
import com.cloudledger.ledger.LedgerService;
import com.cloudledger.outbox.OutboxService;
import com.cloudledger.transaction.Transaction;
import com.cloudledger.transaction.TransactionRepository;
import com.cloudledger.transaction.TransactionStatus;
import com.cloudledger.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferSagaCoordinatorTest {

    @Mock
    private SagaInstanceRepository sagaInstanceRepository;

    @Mock
    private SagaStepRepository sagaStepRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private TransferSagaCoordinator transferSagaCoordinator;

    private UUID srcAccId;
    private UUID destAccId;
    private Account srcAcc;
    private Account destAcc;
    private Transaction tx;

    @BeforeEach
    void setUp() {
        srcAccId = UUID.randomUUID();
        destAccId = UUID.randomUUID();

        srcAcc = new Account();
        srcAcc.setAccountId(srcAccId);
        srcAcc.setAccountNumber("AC-SRC");
        srcAcc.setCurrency("USD");
        srcAcc.setStatus(AccountStatus.ACTIVE);
        srcAcc.setAccountType(AccountType.LIABILITY);

        destAcc = new Account();
        destAcc.setAccountId(destAccId);
        destAcc.setAccountNumber("AC-DEST");
        destAcc.setCurrency("USD");
        destAcc.setStatus(AccountStatus.ACTIVE);
        destAcc.setAccountType(AccountType.LIABILITY);

        tx = new Transaction();
        tx.setSourceAccountId(srcAccId);
        tx.setDestinationAccountId(destAccId);
        tx.setAmount(new BigDecimal("500.0000"));
        tx.setCurrency("USD");
        tx.setTransactionType(TransactionType.TRANSFER);
        tx.setStatus(TransactionStatus.RECEIVED);
    }

    @Test
    @DisplayName("Should successfully orchestrate Saga and mark transaction POSTED")
    void testSuccessfulTransferSaga() {
        when(accountRepository.findById(srcAccId)).thenReturn(Optional.of(srcAcc));
        when(accountRepository.findById(destAccId)).thenReturn(Optional.of(destAcc));
        when(sagaInstanceRepository.save(any(SagaInstance.class))).thenAnswer(inv -> inv.getArgument(0));

        JournalEntry mockJournal = new JournalEntry();
        mockJournal.setStatus(JournalStatus.POSTED);
        when(ledgerService.createJournalEntry(any())).thenReturn(mockJournal);
        when(ledgerService.postJournal(any())).thenReturn(mockJournal);

        transferSagaCoordinator.executeTransferSaga(tx);

        assertEquals(TransactionStatus.POSTED, tx.getStatus());
        verify(accountService, times(1)).reserveFunds(srcAccId, new BigDecimal("500.0000"));
        verify(accountService, times(1)).commitDebit(srcAccId, new BigDecimal("500.0000"));
        verify(accountService, times(1)).commitCredit(destAccId, new BigDecimal("500.0000"));
    }

    @Test
    @DisplayName("Should handle InsufficientBalanceException by marking transaction FAILED without dirty state")
    void testFailedTransferSagaInsufficientFunds() {
        when(accountRepository.findById(srcAccId)).thenReturn(Optional.of(srcAcc));
        when(accountRepository.findById(destAccId)).thenReturn(Optional.of(destAcc));
        when(sagaInstanceRepository.save(any(SagaInstance.class))).thenAnswer(inv -> inv.getArgument(0));

        doThrow(new InsufficientBalanceException("Insufficient funds"))
                .when(accountService).reserveFunds(srcAccId, new BigDecimal("500.0000"));

        transferSagaCoordinator.executeTransferSaga(tx);

        assertEquals(TransactionStatus.FAILED, tx.getStatus());
        assertEquals("INSUFFICIENT_FUNDS", tx.getErrorCode());
        // Verify ledger posting was never called
        verify(ledgerService, never()).createJournalEntry(any());
    }
}
