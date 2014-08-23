package rapid.decoder;

public enum IntegerMaker {
    ROUND {
        @Override
        public int toInteger(float f) {
            return Math.round(f);
        }
    },
    CEIL {
        @Override
        public int toInteger(float f) {
            return (int) Math.ceil(f);
        }
    };

    public abstract int toInteger(float f);
}
