package ru.practicum.shareit.request.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.request.dto.ItemRequestSimpleDto;

import java.util.Map;

@Service
public class ItemRequestClient extends BaseClient {

    private static final String API_PREFIX = "/requests";

    @Autowired
    public ItemRequestClient(@Value("${shareit-server.url}") String serverUrl, RestTemplateBuilder builder) {
        super(builder
                .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl + API_PREFIX))
                .requestFactory(HttpComponentsClientHttpRequestFactory::new)
                .build());
    }

    public ResponseEntity<Object> create(long userId, ItemRequestSimpleDto itemRequestCreateDto) {
        return post("", userId, itemRequestCreateDto);
    }

    public ResponseEntity<Object> getByRequesterId(long requesterId) {
        return get("", requesterId);
    }

    public ResponseEntity<Object> getAll(long requesterId, int from, int size) {
        Map<String, Object> parameters = Map.of("from", from, "size", size);
        return get("/all", requesterId, parameters);
    }

    public ResponseEntity<Object> getById(long requesterId, int requestId) {
        return get("/" + requestId, requesterId);
    }
}
