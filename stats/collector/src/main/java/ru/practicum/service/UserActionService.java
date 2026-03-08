package ru.practicum.service;
import ru.practicum.stats.proto.UserActionProto;

public interface UserActionService {

    void collectUserAction(UserActionProto userActionProto);

}