package br.com.infotech.myfinances.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String login;
    private String name;
    private String type;
    private Boolean changePwdOnLogin;
    private String status;
    private String password;
}
