package ru.practicum.shareit.item.api.service.Test;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import ru.practicum.shareit.constants.Constants;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.api.repository.ItemRepository;
import ru.practicum.shareit.item.api.service.ItemServiceImpl;
import ru.practicum.shareit.item.entity.Item;
import ru.practicum.shareit.user.api.repository.UserRepository;
import ru.practicum.shareit.user.entity.User;

import java.util.Optional;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest
@Disabled
class ItemServiceGetTest {
    @InjectMocks
    private ItemServiceImpl itemService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRepository itemRepository;

    @Test
    @DisplayName("ITEM GET _ THROW IF USER NOT EXIST")
    void get_whenUserNotExists_thenReturnException() {
        long userId = 1;
        long itemId = 1;
        when(userRepository.existsById(userId))
                .thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> itemService.get(userId, itemId),
                format(Constants.USER_NOT_EXISTS, userId));
    }

    @Test
    @DisplayName("ITEM GET _ THROW IF ITEM NOT EXIST")
    void get_whenItemNotExists_thenReturnException() {
        User owner = Constants.RANDOM.nextObject(User.class);
        long ownerId = owner.getId();
        Item item = Constants.RANDOM.nextObject(Item.class);
        item.setOwner(owner);
        long itemId = item.getId();

        when(userRepository.existsById(ownerId))
                .thenReturn(true);
        when(itemRepository.findById(itemId))
                .thenReturn(Optional.empty())
                .thenThrow(new NotFoundException(format(Constants.ITEM_NOT_EXISTS, itemId)));

        assertThrows(NotFoundException.class,
                () -> itemService.get(ownerId, itemId),
                format(Constants.ITEM_NOT_EXISTS, ownerId));
    }
}
