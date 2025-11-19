package org.example;

public class ComponentResponse {
    private Status code;
    private String message;
    private String command;
    private String javaAddress;

    public ComponentResponse(Status code, String message, String command, String javaAddress) {
        this.code = code;
        this.message = message;
        this.command = command;
        this.javaAddress = javaAddress;
    }

    public Status getCode() {
        return code;
    }

    public void setCode(Status code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean isOK() {
        return code.equals(Status.OK);
    }

    @Override
    public String toString() {
        return "ComponentResponse{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", command='" + command + '\'' +
                ", javaAddress='" + javaAddress + '\'' +
                '}';
    }


    public enum Status {
        EXCEPTION_OCCURRED, OK, ERROR_OCCURRED
    }
}
