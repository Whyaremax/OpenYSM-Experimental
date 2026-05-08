package net.minecraftforge.client.event;

public class InputEvent {
    public static class Key extends InputEvent {
        private final int key;
        private final int scanCode;
        private final int action;

        public Key(int key, int scanCode, int action) {
            this.key = key;
            this.scanCode = scanCode;
            this.action = action;
        }

        public int getKey() {
            return key;
        }

        public int getScanCode() {
            return scanCode;
        }

        public int getAction() {
            return action;
        }
    }

    public static class MouseButton extends InputEvent {
        public static class Post extends MouseButton {
            private final int button;
            private final int action;

            public Post(int button, int action) {
                this.button = button;
                this.action = action;
            }

            public int getButton() {
                return button;
            }

            public int getAction() {
                return action;
            }
        }
    }
}
