package com.javagenai.day09.mini_ioc;

public class MiniIoCDemo {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   DAY 09: CUSTOM MINI-IOC CONTAINER DEMO         ");
        System.out.println("==================================================");

        System.out.println("Phase 1: Booting Mini-IoC Container via Reflection...");
        MiniApplicationContext context = new MiniApplicationContext(ChatEngine.class, CustomerBot.class);
        System.out.println();

        System.out.println("Phase 2: Retrieving Wired Bean from Container...");
        CustomerBot bot = context.getBean(CustomerBot.class);

        System.out.println("Phase 3: Invoking AI Business Method...");
        String response = bot.answer("Explain how Spring Inversion of Control works under the hood.");
        System.out.println("Response: " + response);
        System.out.println("==================================================");
    }
}
