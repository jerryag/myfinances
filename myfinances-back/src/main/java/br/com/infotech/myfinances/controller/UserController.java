package br.com.infotech.myfinances.controller;

import br.com.infotech.myfinances.controller.api.IUserController;
import br.com.infotech.myfinances.domain.UserStatus;
import br.com.infotech.myfinances.dto.UserDto;
import br.com.infotech.myfinances.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController implements IUserController {

    private final UserService userService;

    @Override
    @PostMapping(value = "/change-password", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> changePassword(
            @RequestParam("userId") Long userId,
            @RequestParam("oldPassword") String oldPassword,
            @RequestParam("newPassword") String newPassword) {

        userService.changePassword(userId, oldPassword, newPassword);
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping
    public ResponseEntity<Page<UserDto>> findAll(
            @RequestParam(value = "statuses", required = false) List<UserStatus> statuses,
            Pageable pageable) {
        return ResponseEntity.ok(userService.findAll(statuses, pageable));
    }

    @Override
    @PostMapping
    public ResponseEntity<UserDto> create(
            @RequestBody UserDto userDto) {
        return ResponseEntity.ok(userService.create(userDto));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> update(
            @PathVariable("id") Long id,
            @RequestBody UserDto userDto) {
        return ResponseEntity.ok(userService.update(id, userDto));
    }

    @Override
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") UserStatus status) {
        userService.changeStatus(id, status);
        return ResponseEntity.noContent().build();
    }
}
