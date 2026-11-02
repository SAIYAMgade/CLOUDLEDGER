package com.cloudledger.reporting;

import com.cloudledger.common.MoneyUtils;
import com.cloudledger.transaction.Transaction;
import com.cloudledger.transaction.TransactionRepository;
import com.cloudledger.transaction.TransactionStatus;
import com.cloudledger.transaction.TransactionType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class ReportingService {

    private final TransactionRepository transactionRepository;

    public ReportingService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDailyTransactionSummary() {
        List<Transaction> transactions = transactionRepository.findAll();

        long totalCount = transactions.size();
        long successfulCount = 0;
        long failedCount = 0;
        long pendingCount = 0;
        long reversedCount = 0;
        BigDecimal totalVolume = MoneyUtils.ZERO;

        for (Transaction tx : transactions) {
            if (tx.getStatus() == TransactionStatus.POSTED) {
                successfulCount++;
                totalVolume = totalVolume.add(tx.getAmount());
            } else if (tx.getStatus() == TransactionStatus.FAILED) {
                failedCount++;
            } else if (tx.getStatus() == TransactionStatus.REVERSED) {
                reversedCount++;
            } else {
                pendingCount++;
            }
        }

        double successRate = totalCount > 0 ? ((double) successfulCount / totalCount) * 100.0 : 100.0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("date", LocalDate.now().toString());
        summary.put("totalTransactions", totalCount);
        summary.put("successfulTransactions", successfulCount);
        summary.put("failedTransactions", failedCount);
        summary.put("pendingTransactions", pendingCount);
        summary.put("reversedTransactions", reversedCount);
        summary.put("totalProcessedValue", MoneyUtils.format(totalVolume));
        summary.put("successRatePercentage", Math.round(successRate * 100.0) / 100.0);

        return summary;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getTransactionVolumeByType() {
        List<Transaction> transactions = transactionRepository.findAll();
        Map<String, Long> volumeByType = new EnumMap<>(TransactionType.class).keySet().stream()
                .collect(LinkedHashMap::new, (m, t) -> m.put(t.name(), 0L), Map::putAll);

        for (TransactionType type : TransactionType.values()) {
            volumeByType.put(type.name(), 0L);
        }

        for (Transaction tx : transactions) {
            String typeName = tx.getTransactionType().name();
            volumeByType.put(typeName, volumeByType.getOrDefault(typeName, 0L) + 1);
        }

        return volumeByType;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFailedTransactions() {
        Page<Transaction> failed = transactionRepository.findByStatus(
                TransactionStatus.FAILED, PageRequest.of(0, 50, Sort.by("createdAt").descending())
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (Transaction tx : failed) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("transactionId", tx.getTransactionId());
            item.put("externalReference", tx.getExternalReference());
            item.put("amount", tx.getAmount());
            item.put("currency", tx.getCurrency());
            item.put("errorCode", tx.getErrorCode());
            item.put("errorMessage", tx.getErrorMessage());
            item.put("createdAt", tx.getCreatedAt());
            result.add(item);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public String exportTransactionsCsv() {
        List<Transaction> transactions = transactionRepository.findAll(Sort.by("createdAt").descending());
        StringWriter writer = new StringWriter();

        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                .builder()
                .setHeader("Transaction ID", "Reference", "Source Account", "Destination Account", "Amount", "Currency", "Type", "Status", "Error", "Created At")
                .build())) {

            for (Transaction tx : transactions) {
                printer.printRecord(
                    tx.getTransactionId(),
                    tx.getExternalReference(),
                    tx.getSourceAccountId(),
                    tx.getDestinationAccountId(),
                    tx.getAmount(),
                    tx.getCurrency(),
                    tx.getTransactionType(),
                    tx.getStatus(),
                    tx.getErrorMessage() != null ? tx.getErrorMessage() : "",
                    tx.getCreatedAt()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV export", e);
        }

        return writer.toString();
    }
}
