package com.texttosql.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SQLValidation {

    private boolean isValid;
    private String validationMessage;
    private GeneratedSQL generatedSQL;
    
}
