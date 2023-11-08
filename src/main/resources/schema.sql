-- PUBLIC.USERS definition

-- Drop table

drop table IF EXISTS public.users;

CREATE TABLE IF NOT EXISTS public.users (
  id INT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
   email VARCHAR(255),
   name VARCHAR(255),
   CONSTRAINT pk_users PRIMARY KEY (id)
);

ALTER TABLE public.users ADD CONSTRAINT uc_users_email UNIQUE (email);
-- PUBLIC.REQUESTS definition

-- Drop table

drop table IF EXISTS public.requests;

CREATE TABLE IF NOT EXISTS public.requests (
  id INT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
   created date,
   description VARCHAR(2000),
   requester INT,
   CONSTRAINT pk_requests PRIMARY KEY (id)
);

-- PUBLIC.ITEMS definition

-- Drop table

drop table IF EXISTS public.items;

CREATE TABLE IF NOT EXISTS public.items (
  id INT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
   name VARCHAR(255),
   description VARCHAR(255),
   available BOOLEAN NOT NULL,
   owner_id INT,
   CONSTRAINT pk_items PRIMARY KEY (id)
);

-- PUBLIC.BOOKINGS definition

-- Drop table

drop table IF EXISTS public.bookings;

create type BOOKING_STATUS as ENUM ('WAITING', 'APPROVED', 'REJECTED', 'CANCELED');

CREATE TABLE IF NOT EXISTS public.bookings (
  id INT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
   start_timestamp TIMESTAMP,
   end_timestamp TIMESTAMP,
   item_id INT,
   booker_id INT,
   status VARCHAR(255),
   CONSTRAINT pk_bookings PRIMARY KEY (id)
);

ALTER TABLE public.bookings ADD CONSTRAINT FK_BOOKINGS_ON_BOOKER FOREIGN KEY (booker_id) REFERENCES public.users (id);

ALTER TABLE public.bookings ADD CONSTRAINT FK_BOOKINGS_ON_ITEM FOREIGN KEY (item_id) REFERENCES public.items (id);

-- PUBLIC.REQUESTS definition

-- Drop table

drop table IF EXISTS public.comments;

CREATE TABLE IF NOT EXISTS public.comments (
  id INT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
   comment_text VARCHAR(255),
   item_id INT,
   author_id INT,
   CONSTRAINT pk_comments PRIMARY KEY (id)
);