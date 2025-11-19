package org.example;

import static org.example.Constants.API_KEY_ASTERISK_MARGIN;

public class LLM_Config {
    private transient final String apiKey;
    private final double temperature;
    private final double top_p;
    private final double presence_penalty;
    private final double frequency_penalty;
    private Constants.LLM model;

    public LLM_Config(String apiKey, Constants.LLM model,
                      double temperature, double top_p, double presence_penalty, double frequency_penalty) {
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.top_p = top_p;
        this.presence_penalty = presence_penalty;
        this.frequency_penalty = frequency_penalty;
    }

    public LLM_Config(String apiKey, Constants.LLM completionModel) {
        this(apiKey, completionModel, 1, 1, 0, 0);
    }

    public double getTemperature() {
        return temperature;
    }

    public double getTop_p() {
        return top_p;
    }

    public double getPresence_penalty() {
        return presence_penalty;
    }

    public double getFrequency_penalty() {
        return frequency_penalty;
    }

    public Constants.LLM getModel() {
        return model;
    }

    public void setModel(Constants.LLM model) {
        this.model = model;
    }

    public String getApiKey() {
        return apiKey;
    }

    @Override
    public String toString() {
        return
                String.format("API-KEY: %s" +
                                "%s" +
                                "\n" +
                                "Completion Model: %s" +
                                "\n" +
                                "Embeddings Model: %s" +
                                "\n" +
                                "Temperature: %s" +
                                "\n" +
                                "Top_p: %s" +
                                "\n" +
                                "presence_penalty: %s" +
                                "\n" +
                                "frequency_penalty: %s",
                        "*".repeat(apiKey.length() - API_KEY_ASTERISK_MARGIN),
                        apiKey.substring(apiKey.length() - API_KEY_ASTERISK_MARGIN),
                        model,
                        temperature,
                        top_p,
                        presence_penalty,
                        frequency_penalty);
    }
}
