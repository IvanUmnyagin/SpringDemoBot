package io.proj3ct.SpringDemoBot.model;

import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {    //класс, кот будет описывать таблицу, первичный ключ
}
