package dev.magadiflo.app.unitTest.service;

import dev.magadiflo.app.exception.InsufficientMoneyException;
import dev.magadiflo.app.exception.NotFoundEntity;
import dev.magadiflo.app.model.entity.Account;
import dev.magadiflo.app.model.entity.Bank;
import dev.magadiflo.app.repository.AccountRepository;
import dev.magadiflo.app.repository.BankRepository;
import dev.magadiflo.app.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private BankRepository bankRepository;
    @InjectMocks
    private AccountServiceImpl accountService;

    private Long sourceAccountId;
    private Long targetAccountId;
    private Long bankId;
    private BigDecimal amount;
    private Account sourceAccount;
    private Account targetAccount;
    private Bank bank;

    @BeforeEach
    void setUp() {
        this.sourceAccountId = 1L;
        this.targetAccountId = 2L;
        this.bankId = 1L;
        this.amount = new BigDecimal("700");
        this.sourceAccount = new Account(1L, "Martín", new BigDecimal("2000"));
        this.targetAccount = new Account(2L, "Alicia", new BigDecimal("1000"));
        this.bank = new Bank(1L, "Banco de la Nación", 0);
    }

    @Test
    void shouldTransferAmountBetweenAccounts() {
        // given
        when(this.accountRepository.findById(this.sourceAccountId)).thenReturn(Optional.of(this.sourceAccount));
        when(this.accountRepository.findById(this.targetAccountId)).thenReturn(Optional.of(this.targetAccount));
        when(this.bankRepository.findById(this.bankId)).thenReturn(Optional.of(this.bank));

        // when
        this.accountService.transfer(this.bankId, this.sourceAccountId, this.targetAccountId, this.amount);

        // then
        assertEquals(BigDecimal.valueOf(1300), this.sourceAccount.getBalance());
        assertEquals(BigDecimal.valueOf(1700), this.targetAccount.getBalance());
        assertEquals(1, this.bank.getTotalTransfers());
        verify(this.accountRepository).findById(this.sourceAccountId);
        verify(this.accountRepository).findById(this.targetAccountId);
        verify(this.accountRepository, times(2)).findById(anyLong());
        verify(this.bankRepository).findById(this.bankId);
        verify(this.accountRepository).save(this.sourceAccount);
        verify(this.accountRepository).save(this.targetAccount);
        verify(this.accountRepository, times(2)).save(any(Account.class));
        verify(this.bankRepository).save(this.bank);
    }

    @Test
    void shouldThrowAnExceptionWhenTheAmountToBeTransferredIsGreaterThanTheAvailableBalance() {
        // given
        this.amount = new BigDecimal("5000");
        when(this.accountRepository.findById(this.sourceAccountId)).thenReturn(Optional.of(this.sourceAccount));
        when(this.accountRepository.findById(this.targetAccountId)).thenReturn(Optional.of(this.targetAccount));
        when(this.bankRepository.findById(this.bankId)).thenReturn(Optional.of(bank));

        // when
        InsufficientMoneyException exception = assertThrows(InsufficientMoneyException.class, () -> {
            this.accountService.transfer(this.bankId, this.sourceAccountId, this.targetAccountId, this.amount);
        });

        // then
        assertEquals(InsufficientMoneyException.class, exception.getClass());
        assertEquals("El saldo es insuficiente", exception.getMessage());
        assertEquals(BigDecimal.valueOf(2000), this.sourceAccount.getBalance());
        assertEquals(BigDecimal.valueOf(1000), this.targetAccount.getBalance());
        assertEquals(0, this.bank.getTotalTransfers());
        verify(this.accountRepository).findById(this.sourceAccountId);
        verify(this.accountRepository).findById(this.targetAccountId);
        verify(this.accountRepository, times(2)).findById(anyLong());
        verify(this.bankRepository).findById(this.bankId);
        verify(this.accountRepository, never()).save(any(Account.class));
        verify(this.bankRepository, never()).save(this.bank);
    }

    @Test
    void shouldFindAccountById() {
        // given
        when(this.accountRepository.findById(1L)).thenReturn(Optional.of(this.sourceAccount));

        // when
        Optional<Account> accountDB = this.accountService.findById(1L);

        // then
        assertTrue(accountDB.isPresent());
        assertSame(this.sourceAccount, accountDB.get());
        verify(this.accountRepository).findById(1L);
    }

    @Test
    void shouldThrowAnExceptionWhenTheSourceAccountDoesNotExist() {
        // given
        when(this.accountRepository.findById(this.sourceAccountId)).thenReturn(Optional.empty());

        // when
        NotFoundEntity notFoundEntity = assertThrows(NotFoundEntity.class, () -> {
            this.accountService.transfer(this.bankId, this.sourceAccountId, this.targetAccountId, this.amount);
        });

        // then
        assertEquals(NotFoundEntity.class, notFoundEntity.getClass());
        assertEquals("No existe la cuenta de origen con id " + this.sourceAccountId, notFoundEntity.getMessage());
        assertEquals(BigDecimal.valueOf(1000), this.targetAccount.getBalance());
        assertEquals(0, this.bank.getTotalTransfers());
        verify(this.accountRepository).findById(sourceAccountId);
        verify(this.accountRepository, never()).findById(targetAccountId);
        verify(this.accountRepository, times(1)).findById(anyLong());
        verify(this.bankRepository, never()).findById(bankId);
        verify(this.accountRepository, never()).save(any(Account.class));
        verify(this.bankRepository, never()).save(this.bank);
    }

    @Test
    void shouldThrowAnExceptionWhenTheTargetAccountDoesNotExist() {
        // given
        when(this.accountRepository.findById(this.sourceAccountId)).thenReturn(Optional.of(this.sourceAccount));
        when(this.accountRepository.findById(this.targetAccountId)).thenReturn(Optional.empty());

        // when
        NotFoundEntity notFoundEntity = assertThrows(NotFoundEntity.class, () -> {
            this.accountService.transfer(this.bankId, this.sourceAccountId, this.targetAccountId, this.amount);
        });

        // then
        assertEquals(NotFoundEntity.class, notFoundEntity.getClass());
        assertEquals("No existe la cuenta de destino con id " + this.targetAccountId, notFoundEntity.getMessage());
        assertEquals(BigDecimal.valueOf(2000), this.sourceAccount.getBalance());
        assertEquals(0, this.bank.getTotalTransfers());
        verify(this.accountRepository).findById(this.sourceAccountId);
        verify(this.accountRepository).findById(this.targetAccountId);
        verify(this.accountRepository, times(2)).findById(anyLong());
        verify(this.bankRepository, never()).findById(this.bankId);
        verify(this.accountRepository, never()).save(any(Account.class));
        verify(this.bankRepository, never()).save(this.bank);
    }

    @Test
    void shouldThrowAnExceptionWhenTheBankDoesNotExist() {
        // given
        when(this.accountRepository.findById(this.sourceAccountId)).thenReturn(Optional.of(this.sourceAccount));
        when(this.accountRepository.findById(this.targetAccountId)).thenReturn(Optional.of(this.targetAccount));
        when(this.bankRepository.findById(this.bankId)).thenReturn(Optional.empty());

        // when
        NotFoundEntity notFoundEntity = assertThrows(NotFoundEntity.class, () -> {
            this.accountService.transfer(this.bankId, this.sourceAccountId, this.targetAccountId, this.amount);
        });

        // then
        assertEquals(NotFoundEntity.class, notFoundEntity.getClass());
        assertEquals("No existe el banco con el id " + this.bankId, notFoundEntity.getMessage());
        assertEquals(BigDecimal.valueOf(2000), this.sourceAccount.getBalance());
        assertEquals(BigDecimal.valueOf(1000), this.targetAccount.getBalance());
        verify(this.accountRepository).findById(this.sourceAccountId);
        verify(this.accountRepository).findById(this.targetAccountId);
        verify(this.accountRepository, times(2)).findById(anyLong());
        verify(this.bankRepository).findById(this.bankId);
        verify(this.accountRepository, never()).save(any(Account.class));
        verify(this.bankRepository, never()).save(any(Bank.class));
    }

    @Test
    void shouldReturnAnOptionalEmptyWhenTheAccountDoesNotExist() {
        // given
        when(this.accountRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        Optional<Account> accountDB = this.accountService.findById(1L);

        // then
        assertTrue(accountDB.isEmpty());
        verify(this.accountRepository).findById(1L);
    }

    @Test
    void shouldGetTheBalanceOfAnAccount() {
        // given
        when(this.accountRepository.findById(1L)).thenReturn(Optional.of(this.sourceAccount));

        // when
        BigDecimal balance = this.accountService.reviewBalance(1L);

        // then
        assertEquals(BigDecimal.valueOf(2000), balance);
        verify(this.accountRepository).findById(1L);
        verify(this.accountRepository, times(1)).findById(anyLong());
    }

    @Test
    void shouldThrowAnExceptionWhenAccountDoesNotExist() {
        // given
        when(this.accountRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        NotFoundEntity exception = assertThrows(NotFoundEntity.class, () -> {
            this.accountService.reviewBalance(1L);
        });

        // then
        assertEquals(NotFoundEntity.class, exception.getClass());
        assertEquals("No existe la cuenta con el id 1", exception.getMessage());
        verify(this.accountRepository).findById(1L);
        verify(this.accountRepository, times(1)).findById(anyLong());
    }

    @Test
    void shouldGetTheTotalTransfersFromTheBank() {
        // given
        this.bank.setTotalTransfers(10);
        when(this.bankRepository.findById(1L)).thenReturn(Optional.of(this.bank));

        // when
        int total = this.accountService.reviewTotalTransfers(1L);

        // then
        assertEquals(10, total);
        verify(this.bankRepository).findById(1L);
        verify(this.bankRepository, times(1)).findById(anyLong());
    }

    @Test
    void shouldThrowAnExceptionWhenTheBankDoesNotExistWhenReviewingTheTotalTransfers() {
        // given
        when(this.bankRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        NotFoundEntity exception = assertThrows(NotFoundEntity.class, () -> {
            this.accountService.reviewTotalTransfers(1L);
        });

        // then
        assertEquals(NotFoundEntity.class, exception.getClass());
        assertEquals("No existe el banco con el id 1", exception.getMessage());
        verify(this.bankRepository).findById(1L);
        verify(this.bankRepository, times(1)).findById(anyLong());
    }

    @Test
    void shouldFindAllAccounts() {
        // given
        List<Account> accounts = List.of(this.sourceAccount, this.targetAccount);
        when(this.accountRepository.findAll()).thenReturn(accounts);

        // when
        List<Account> accountList = this.accountService.findAll();

        // then
        assertFalse(accountList.isEmpty());
        assertEquals(accounts.size(), accountList.size());
        verify(this.accountRepository).findAll();
    }

    @Test
    void shouldSaveAnAccount() {
        // given
        Account accountToSave = Account.builder()
                .person("Milagros")
                .balance(new BigDecimal("2500"))
                .build();
        when(this.accountRepository.save(any(Account.class))).then(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(1L);
            return account;
        });

        // when
        Account accountDB = this.accountService.save(accountToSave);

        // then
        assertNotNull(accountDB);
        assertNotNull(accountDB.getId());
        assertEquals(1L, accountDB.getId());
        assertEquals(accountToSave.getPerson(), accountDB.getPerson());
        assertEquals(accountToSave.getBalance().doubleValue(), accountDB.getBalance().doubleValue());
        verify(this.accountRepository).save(any(Account.class));
    }

    @Test
    void shouldDeleteAnAccount() {
        // given
        Long accountToDeleteId = 1L;
        when(this.accountRepository.findById(accountToDeleteId)).thenReturn(Optional.of(this.sourceAccount));
        when(this.accountRepository.deleteAccountById(accountToDeleteId)).thenReturn(1);

        // when
        Optional<Boolean> wasDeleted = this.accountService.deleteAccountById(accountToDeleteId);

        // then
        assertTrue(wasDeleted.isPresent());
        verify(this.accountRepository).findById(accountToDeleteId);
        verify(this.accountRepository).deleteAccountById(accountToDeleteId);
    }

    @Test
    void shouldReturnOptionalEmptyWhenDeleteAnAccountThatDoesNotExit() {
        // given
        Long accountToDeleteId = 1L;
        when(this.accountRepository.findById(accountToDeleteId)).thenReturn(Optional.empty());

        // when
        Optional<Boolean> wasDeleted = this.accountService.deleteAccountById(accountToDeleteId);

        // then
        assertTrue(wasDeleted.isEmpty());
        verify(this.accountRepository).findById(accountToDeleteId);
        verify(this.accountRepository, never()).deleteAccountById(anyLong());
    }
}