create table messages(
    id serial primary key,
    content text,
    created_at timestamp default current_timestamp
);

insert into messages(content) values('Hello, world!');
insert into messages(content) values('Hallo, wereld!');
insert into messages(content) values('Olá, mundo!');
insert into messages(content) values('¡Hola, mundo!');
insert into messages(content) values('Bonjour, le monde!');
insert into messages(content) values('Hallo, Welt!');
insert into messages(content) values('Ciao, mondo!');
