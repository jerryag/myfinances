package br.com.infotech.myfinances.controller;

import br.com.infotech.myfinances.dto.UserDto;
import br.com.infotech.myfinances.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class LoginController implements br.com.infotech.myfinances.controller.api.ILoginController {

    private final UserService userService;

    public LoginController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Override
    public ResponseEntity<UserDto> login(
            @RequestParam("login") String login,
            @RequestParam("password") String password) {

        UserDto userDto = userService.login(login, password);
        return ResponseEntity.ok(userDto);
    }
}
