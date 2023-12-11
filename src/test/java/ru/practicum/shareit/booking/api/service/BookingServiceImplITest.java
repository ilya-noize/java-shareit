package ru.practicum.shareit.booking.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.practicum.shareit.booking.api.dto.BookingDto;
import ru.practicum.shareit.booking.api.dto.BookingSimpleDto;
import ru.practicum.shareit.booking.api.repository.BookingRepository;
import ru.practicum.shareit.booking.entity.Booking;
import ru.practicum.shareit.booking.entity.enums.BookingStatus;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.BookingException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.RentalPeriodException;
import ru.practicum.shareit.item.api.repository.ItemRepository;
import ru.practicum.shareit.item.entity.Item;
import ru.practicum.shareit.user.api.repository.UserRepository;
import ru.practicum.shareit.user.entity.User;

import java.time.LocalDateTime;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;
import static ru.practicum.shareit.ShareItApp.*;
import static ru.practicum.shareit.booking.entity.enums.BookingStatus.*;

@SpringBootTest
class BookingServiceImplITest {
    private final LocalDateTime now = LocalDateTime.now();

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private BookingService bookingService;

    private User getNewUser() {
        User owner = RANDOM.nextObject(User.class);
        return userRepository.save(owner);
    }

    private Item getNewItem(User owner, boolean available) {
        Item item = RANDOM.nextObject(Item.class);
        item.setOwner(owner);
        item.setAvailable(available);
        item.setRequest(null);

        return itemRepository.save(item);
    }

    private Booking getNewBooking(LocalDateTime start, LocalDateTime end, Item item, User booker, BookingStatus status) {
        Booking booking = Booking.builder()
                .start(start)
                .end(end)
                .item(item)
                .booker(booker)
                .status(status).build();
        return bookingRepository.save(booking);
    }

    @Test
    void create_ItemIsNotExists_Throw() {
        User booker = getNewUser();
        int itemId = Integer.MAX_VALUE;

        LocalDateTime start = now.minusDays(7);
        LocalDateTime end = now.minusDays(4);

        BookingSimpleDto bookingSimpleDto =
                new BookingSimpleDto(null, start, end, itemId);

        assertThrows(NotFoundException.class, () ->
                        bookingService.create(booker.getId(), bookingSimpleDto),
                format(ITEM_NOT_EXISTS, itemId));
    }

    @Test
    void create_ItemIsNotAvailable_Throw() {
        User owner = getNewUser();
        Item item = getNewItem(owner, false);

        User booker = getNewUser();

        LocalDateTime start = now.minusDays(7);
        LocalDateTime end = now.minusDays(4);

        BookingSimpleDto bookingSimpleDto =
                new BookingSimpleDto(null, start, end, item.getId());

        assertThrows(BadRequestException.class, () ->
                        bookingService.create(booker.getId(), bookingSimpleDto),
                "It is impossible to rent an item to which access is closed.");
    }

    @Test
    void create_BookerIsOwner_Throw() {
        User owner = getNewUser();
        Item item = getNewItem(owner, true);

        Integer bookerId = owner.getId();

        LocalDateTime start = now.minusDays(7);
        LocalDateTime end = now.minusDays(4);

        BookingSimpleDto bookingSimpleDto =
                new BookingSimpleDto(null, start, end, item.getId());

        assertThrows(BookingException.class, () ->
                        bookingService.create(bookerId, bookingSimpleDto),
                "Access denied. You are owner this item");
    }

    @Test
    void create_startEqualsEnd_Throw() {
        User owner = getNewUser();
        Item item = getNewItem(owner, true);
        User booker = getNewUser();

        LocalDateTime start = now.minusDays(4);
        LocalDateTime end = now.minusDays(4);

        BookingSimpleDto bookingSimpleDto =
                new BookingSimpleDto(null, start, end, item.getId());

        assertThrows(RentalPeriodException.class, () ->
                        bookingService.create(booker.getId(), bookingSimpleDto),
                "The effective date of the lease agreement" +
                        " coincides with its termination");
    }

    @Test
    void create_startAfterEnd_Throw() {
        User owner = getNewUser();
        Item item = getNewItem(owner, true);
        User booker = getNewUser();

        LocalDateTime start = now.minusDays(4);
        LocalDateTime end = now.minusDays(4);

        BookingSimpleDto bookingSimpleDto =
                new BookingSimpleDto(null, start, end, item.getId());

        assertThrows(RentalPeriodException.class, () ->
                        bookingService.create(booker.getId(), bookingSimpleDto),
                "The effective date of the lease agreement" +
                        " after its termination");
    }

    @Test
    void create() {
        User owner = getNewUser();
        Item item = getNewItem(owner, true);
        User booker = getNewUser();

        LocalDateTime start = now.minusDays(7);
        LocalDateTime end = now.minusDays(4);

        Integer itemId = item.getId();

        BookingSimpleDto bookingSimpleDto =
                new BookingSimpleDto(null, start, end, itemId);

        Integer bookerId = booker.getId();

        BookingDto response = bookingService.create(bookerId, bookingSimpleDto);

        assertNotNull(response.getId());
    }

    @Test
    void update_BookingNotExists_Throw() {
        User booker = getNewUser();

        Integer bookerId = booker.getId();
        Long bookingId = Long.MAX_VALUE;

        assertThrows(NotFoundException.class, () ->
                        bookingService.update(bookerId, bookingId, true),
                format(BOOKING_NOT_EXISTS, bookingId));
    }


    @Test
    void update_BookingStatusNotWaiting_Throw() {
        User owner = getNewUser();
        Item item = getNewItem(owner, true);
        User booker = getNewUser();

        Booking booking = getNewBooking(
                now.plusDays(1),
                now.plusDays(2),
                item,
                booker,
                REJECTED);

        Integer bookerId = booker.getId();
        Long bookingId = booking.getId();

        assertThrows(BadRequestException.class, () ->
                        bookingService.update(bookerId, bookingId, true),
                "The booking status has already been set.");
    }

    @Test
    void update_userNotExists_Throw() {
        User owner = getNewUser();
        Item item = getNewItem(owner, true);
        User booker = getNewUser();

        Booking booking = getNewBooking(
                now.plusDays(1),
                now.plusDays(2),
                item,
                booker,
                WAITING);

        Integer bookerId = Integer.MAX_VALUE;
        Long bookingId = booking.getId();

        assertThrows(NotFoundException.class, () ->
                        bookingService.update(bookerId, bookingId, true),
                format(USER_NOT_EXISTS, bookerId));
    }

    @Test
    void update_bookerIsOwner_Throw() {
        User owner = getNewUser();
        Item item = getNewItem(owner, true);

        Booking booking = getNewBooking(
                now.plusDays(1),
                now.plusDays(2),
                item,
                owner,
                WAITING);

        Integer bookerId = owner.getId();
        Long bookingId = booking.getId();

        assertThrows(BookingException.class, () ->
                        bookingService.update(bookerId, bookingId, true),
                "Access denied.\n"
                        + "You cannot be the owner and the booker of this item at the same time.");
    }

    @Test
    void update_approved() {
        User owner = getNewUser();
        Item item = getNewItem(owner, true);
        User booker = getNewUser();

        Booking booking = getNewBooking(
                now.plusDays(1),
                now.plusDays(2),
                item,
                booker,
                WAITING);

        Integer ownerId = owner.getId();
        Long bookingId = booking.getId();

        BookingDto result = bookingService.update(ownerId, bookingId, true);
        assertEquals(APPROVED, result.getStatus());
    }

    @Test
    void update_rejected() {
        User owner = getNewUser();
        Item item = getNewItem(owner, true);
        User booker = getNewUser();

        Booking booking = getNewBooking(
                now.plusDays(1),
                now.plusDays(2),
                item,
                booker,
                WAITING);

        Integer ownerId = owner.getId();
        Long bookingId = booking.getId();

        BookingDto result = bookingService.update(ownerId, bookingId, false);
        assertEquals(REJECTED, result.getStatus());
    }

    @Test
    void get_NotFound_Throw() {
        User owner = getNewUser();
        getNewItem(owner, true);
        User booker = getNewUser();

        Integer bookerId = booker.getId();
        Long bookingId = Long.MAX_VALUE;

        assertThrows(NotFoundException.class, () ->
                        bookingService.get(bookerId, bookingId),
                format(BOOKING_NOT_EXISTS, bookingId));
    }

    @Test
    void get_NotFoundBooker_Throw() {
        User owner = getNewUser();
        Item item = getNewItem(owner, true);
        User booker = getNewUser();

        Booking booking = getNewBooking(
                now.plusDays(1),
                now.plusDays(2),
                item,
                booker,
                WAITING);

        Integer bookerId = Integer.MAX_VALUE;
        Long bookingId = booking.getId();

        assertThrows(NotFoundException.class, () ->
                        bookingService.get(bookerId, bookingId),
                format(USER_NOT_EXISTS, bookingId));
    }

    @Test
    void get_userNotBookerOrOwner_Throw() {
        User owner = getNewUser();
        Item item = getNewItem(owner, true);
        User booker = getNewUser();
        User requester = getNewUser();

        Booking booking = getNewBooking(
                now.plusDays(1),
                now.plusDays(2),
                item,
                booker,
                WAITING);

        Integer bookerId = requester.getId();
        Long bookingId = booking.getId();

        assertThrows(BookingException.class, () ->
                        bookingService.get(bookerId, bookingId),
                "Access denied.\n"
                        + "You a not the booker/owner of the item");
    }

    @Test
    void get_forBooker() {
        User owner = getNewUser();
        Item item = getNewItem(owner, true);
        User booker = getNewUser();

        Booking booking = getNewBooking(
                now.plusDays(1),
                now.plusDays(2),
                item,
                booker,
                WAITING);

        Integer bookerId = booker.getId();
        Long bookingId = booking.getId();

        BookingDto result = bookingService.get(bookerId, bookingId);
        assertEquals(bookerId, result.getBooker().getId());
    }

    @Test
    void get_forOwner() {
        User owner = getNewUser();
        Item item = getNewItem(owner, true);
        User booker = getNewUser();


        Booking booking = getNewBooking(
                now.plusDays(1),
                now.plusDays(2),
                item,
                booker,
                WAITING);

        Integer ownerId = owner.getId();
        Long bookingId = booking.getId();

        BookingDto result = bookingService.get(ownerId, bookingId);
        assertEquals(ownerId, item.getOwner().getId());
        assertEquals(item.getName(), result.getItem().getName());
    }
}