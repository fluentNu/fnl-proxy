package utils;

public class MessageBuild {
    public static Message onlyType(int type) {
        Message message = new Message();
        message.setType(type);
        return message;
    }
}
