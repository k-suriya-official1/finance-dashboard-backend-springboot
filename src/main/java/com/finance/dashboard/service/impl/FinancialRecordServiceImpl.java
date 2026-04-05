package com.finance.dashboard.service.impl;

import com.finance.dashboard.dto.request.FinancialRecordRequest;
import com.finance.dashboard.dto.response.FinancialRecordResponse;
import com.finance.dashboard.dto.response.PagedResponse;
import com.finance.dashboard.entity.FinancialRecord;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.TransactionType;
import com.finance.dashboard.exception.ResourceNotFoundException;
import com.finance.dashboard.repository.FinancialRecordRepository;
import com.finance.dashboard.repository.UserRepository;
import com.finance.dashboard.service.FinancialRecordService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class FinancialRecordServiceImpl implements FinancialRecordService {

    private final FinancialRecordRepository recordRepository;
    private final UserRepository userRepository;

 
    public FinancialRecordServiceImpl(FinancialRecordRepository recordRepository,
                                      UserRepository userRepository) {
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public FinancialRecordResponse createRecord(FinancialRecordRequest request, String creatorEmail) {

        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + creatorEmail));

        FinancialRecord record = new FinancialRecord();
        record.setAmount(request.getAmount());
        record.setType(request.getType());
        record.setCategory(request.getCategory().trim());
        record.setTransactionDate(request.getTransactionDate());
        record.setNotes(request.getNotes());
        record.setCreatedBy(creator);
        record.setDeleted(false);

        return toResponse(recordRepository.save(record));
    }

   

    @Override
    @Transactional(readOnly = true)
    public FinancialRecordResponse getRecordById(Long id) {
        FinancialRecord record = recordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialRecord", id));
        return toResponse(record);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<FinancialRecordResponse> getAllRecords(
            TransactionType type,
            String category,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {

        Page<FinancialRecordResponse> page = recordRepository
                .findAllWithFilters(type, category, from, to, pageable)
                .map(this::toResponse);

        return PagedResponse.from(page);
    }



    @Override
    @Transactional
    public FinancialRecordResponse updateRecord(Long id, FinancialRecordRequest request) {

        FinancialRecord record = recordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialRecord", id));

        record.setAmount(request.getAmount());
        record.setType(request.getType());
        record.setCategory(request.getCategory().trim());
        record.setTransactionDate(request.getTransactionDate());
        record.setNotes(request.getNotes());

        return toResponse(recordRepository.save(record));
    }



    @Override
    @Transactional
    public void deleteRecord(Long id) {

        FinancialRecord record = recordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialRecord", id));

        record.setDeleted(true);
        recordRepository.save(record);
    }



    public FinancialRecordResponse toResponse(FinancialRecord record) {

        return new FinancialRecordResponse(
                record.getId(),
                record.getAmount(),
                record.getType(),
                record.getCategory(),
                record.getTransactionDate(),
                record.getNotes(),
                record.getCreatedBy().getName(),
                record.getCreatedBy().getEmail(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}