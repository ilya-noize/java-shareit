package ru.practicum.shareit.request.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.request.api.client.ItemRequestClient;
import ru.practicum.shareit.request.api.dto.ItemRequestSimpleDto;
import ru.practicum.shareit.valid.group.Create;

import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import static ru.practicum.shareit.constants.Constants.CREATE_REQUEST;
import static ru.practicum.shareit.constants.Constants.FROM;
import static ru.practicum.shareit.constants.Constants.GET_ALL_REQUESTS;
import static ru.practicum.shareit.constants.Constants.GET_BY_REQUESTER;
import static ru.practicum.shareit.constants.Constants.GET_REQUEST;
import static ru.practicum.shareit.constants.Constants.HEADER_USER_ID;
import static ru.practicum.shareit.constants.Constants.SIZE;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ItemRequestController {
    private final ItemRequestClient itemRequestClient;

    @PostMapping(CREATE_REQUEST)
    public ResponseEntity<Object> create(
            @RequestHeader(HEADER_USER_ID) int requesterId,
            @RequestBody
            @Validated(Create.class) ItemRequestSimpleDto itemRequestSimpleDto) {

        return itemRequestClient.create(requesterId, itemRequestSimpleDto);
    }

    @GetMapping(GET_BY_REQUESTER)
    public ResponseEntity<Object> getByRequesterId(
            @RequestHeader(HEADER_USER_ID) int requesterId) {

        return itemRequestClient.getByRequesterId(requesterId);
    }

    @GetMapping(GET_ALL_REQUESTS)
    @Validated
    public ResponseEntity<Object> getAll(
            @RequestHeader(HEADER_USER_ID) int requesterId,
            @RequestParam(required = false, defaultValue = FROM)
            @PositiveOrZero Integer from,
            @RequestParam(required = false, defaultValue = SIZE)
            @Positive Integer size) {

        return itemRequestClient.getAll(requesterId, from, size);
    }

    @GetMapping(GET_REQUEST)
    public ResponseEntity<Object> getById(
            @RequestHeader(HEADER_USER_ID) int requesterId,
            @PathVariable int requestId) {

        return itemRequestClient.getById(requesterId, requestId);
    }
}