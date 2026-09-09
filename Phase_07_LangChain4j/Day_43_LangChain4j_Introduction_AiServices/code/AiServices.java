package com.genai.langchain4j.aiservices;

import java.lang.reflect.*;
import java.util.*;

/**
 * Declarative AI Services Factory.
 * Generates runtime dynamic proxies implementing user-defined Java interfaces,
 * mapping method calls to model prompts, parameter substitution, and return-type conversions.
 * Matches dev.langchain4j.service.AiServices.
 */
public class AiServices<T> {

    private final Class<T> aiServiceClass;
    private ChatLanguageModel chatLanguageModel;

    private AiServices(Class<T> aiServiceClass) {
        this.aiServiceClass = aiServiceClass;
    }

    public static <T> AiServices<T> builder(Class<T> aiServiceClass) {
        return new AiServices<>(aiServiceClass);
    }

    public static <T> T create(Class<T> aiServiceClass, ChatLanguageModel chatLanguageModel) {
        return builder(aiServiceClass).chatLanguageModel(chatLanguageModel).build();
    }

    public AiServices<T> chatLanguageModel(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
        return this;
    }

    @SuppressWarnings("unchecked")
    public T build() {
        Objects.requireNonNull(chatLanguageModel, "ChatLanguageModel must not be null");

        return (T) Proxy.newProxyInstance(
            aiServiceClass.getClassLoader(),
            new Class<?>[]{aiServiceClass},
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getDeclaringClass() == Object.class) {
                        return method.invoke(this, args);
                    }

                    // 1. Resolve System Prompt
                    String systemText = "";
                    if (method.isAnnotationPresent(SystemMessage.class)) {
                        systemText = method.getAnnotation(SystemMessage.class).value();
                    } else if (aiServiceClass.isAnnotationPresent(SystemMessage.class)) {
                        systemText = aiServiceClass.getAnnotation(SystemMessage.class).value();
                    }

                    // 2. Resolve User Template and @V Variables
                    String userTemplate = "";
                    Map<String, String> variables = new HashMap<>();

                    if (method.isAnnotationPresent(UserMessage.class)) {
                        userTemplate = method.getAnnotation(UserMessage.class).value();
                    }

                    Parameter[] params = method.getParameters();
                    for (int i = 0; i < params.length; i++) {
                        Parameter p = params[i];
                        Object val = (args != null && i < args.length) ? args[i] : "";

                        if (p.isAnnotationPresent(V.class)) {
                            variables.put(p.getAnnotation(V.class).value(), String.valueOf(val));
                        } else if (p.isAnnotationPresent(UserMessage.class) || userTemplate.isEmpty()) {
                            userTemplate = String.valueOf(val);
                        }
                    }

                    // Substitute {{var}} placeholders
                    for (Map.Entry<String, String> entry : variables.entrySet()) {
                        userTemplate = userTemplate.replace("{{" + entry.getKey() + "}}", entry.getValue());
                    }

                    // 3. Assemble chat turns
                    List<ChatMessage> messages = new ArrayList<>();
                    if (!systemText.isBlank()) {
                        messages.add(ChatMessage.system(systemText));
                    }
                    messages.add(ChatMessage.user(userTemplate));

                    // 4. Dispatch to model
                    ModelResponse response = chatLanguageModel.generate(messages);

                    // 5. Automatic Return Type Conversion
                    Class<?> returnType = method.getReturnType();
                    if (returnType == String.class) {
                        return response.content();
                    } else if (returnType.isEnum()) {
                        for (Object constant : returnType.getEnumConstants()) {
                            if (constant.toString().equalsIgnoreCase(response.content().trim())) {
                                return constant;
                            }
                        }
                        return returnType.getEnumConstants()[0]; // default fallback
                    } else if (returnType == ModelResponse.class) {
                        return response;
                    }

                    return response.content();
                }
            }
        );
    }
}
