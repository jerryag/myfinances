package br.com.infotech.myfinances.dto;

import java.math.BigDecimal;
import br.com.infotech.myfinances.domain.TransactionTypeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionTypeDto {

    private Long id;

    @NotNull
    private TransactionTypeType type;

    @NotBlank
    private String description;

    @NotNull
    private Boolean recurring;

    private Integer defaultDay;

    private BigDecimal defaultAmount;

    private String iconName;
}
