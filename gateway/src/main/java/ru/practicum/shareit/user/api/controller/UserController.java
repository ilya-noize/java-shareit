package ru.practicum.shareit.user.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.user.api.client.UserClient;
import ru.practicum.shareit.user.api.dto.UserDto;
import ru.practicum.shareit.user.api.dto.UserSimpleDto;
import ru.practicum.shareit.valid.group.Create;
import ru.practicum.shareit.valid.group.Update;

import javax.validation.constraints.Positive;

import static ru.practicum.shareit.constants.Constants.CREATE_USER;
import static ru.practicum.shareit.constants.Constants.DELETE_USER;
import static ru.practicum.shareit.constants.Constants.GET_ALL_USERS;
import static ru.practicum.shareit.constants.Constants.GET_USER;
import static ru.practicum.shareit.constants.Constants.UPDATE_USER;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserClient userClient;


    @PostMapping(CREATE_USER)
    public ResponseEntity<Object> create(
            @RequestBody @Validated(Create.class) UserSimpleDto userDto) {
        log.info("[i] create user {}", userDto);

        return userClient.create(userDto);
    }

    @PatchMapping(UPDATE_USER)
    public ResponseEntity<Object> update(
            @PathVariable @Positive Long id,
            @RequestBody @Validated(Update.class) UserDto userDto) {
        log.info("[i] update user {}", userDto);

        return userClient.update(id, userDto);
    }

    @GetMapping(GET_USER)
    @Validated
    public ResponseEntity<Object> getUsersById(
            @PathVariable Long id) {
        log.info("[i] get user {}", id);

        return userClient.getById(id);
    }

    @GetMapping(GET_ALL_USERS)
    public ResponseEntity<Object> getAll() {
        log.info("[i] get users");

        return userClient.getAll();
    }

    @DeleteMapping(DELETE_USER)
    @Validated
    public void deleteUser(
            @PathVariable Long id) {
        log.info("[i] delete user {}", id);
        userClient.delete(id);
    }

}