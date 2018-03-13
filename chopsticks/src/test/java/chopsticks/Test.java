package chopsticks;

import com.chopsticks.Chopsticks;

public class Test {
    public static void main(String[] args) {
        Chopsticks.me()
        .get("/", (request, response) -> response.text("Hello World"))
        .start(Test.class, args);
    }
}
