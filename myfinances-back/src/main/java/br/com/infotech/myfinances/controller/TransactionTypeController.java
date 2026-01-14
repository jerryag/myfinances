package br.com.infotech.myfinances.controller;

import br.com.infotech.myfinances.dto.TransactionTypeDto;
import br.com.infotech.myfinances.service.TransactionTypeService;
import br.com.infotech.myfinances.controller.api.ITransactionTypeController;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TransactionTypeController implements ITransactionTypeController {

    private final TransactionTypeService transactionTypeService;

    @Override
    public ResponseEntity<Page<TransactionTypeDto>> findAll(
            String description,
            List<String> types,
            @PageableDefault(size = 10, sort = "description") Pageable pageable) {
        return ResponseEntity.ok(transactionTypeService.findAll(description, types, pageable));
    }

    @Override
    public ResponseEntity<TransactionTypeDto> findById(Long id) {
        return ResponseEntity.ok(transactionTypeService.findById(id));
    }

    @Override
    public ResponseEntity<TransactionTypeDto> create(TransactionTypeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionTypeService.create(dto));
    }

    @Override
    public ResponseEntity<TransactionTypeDto> update(Long id, TransactionTypeDto dto) {
        return ResponseEntity.ok(transactionTypeService.update(id, dto));
    }

    @Override
    public ResponseEntity<Void> delete(Long id) {
        transactionTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
