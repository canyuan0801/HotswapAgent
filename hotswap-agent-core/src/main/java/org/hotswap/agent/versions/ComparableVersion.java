package org.hotswap.agent.versions;



import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Stack;



public class ComparableVersion implements Comparable<ComparableVersion> {
    
    
    private String value;

    
    private String canonical;

    
    private ListItem items;

    
    private interface Item {
        
        
        int INTEGER_ITEM = 0;
        
        
        int STRING_ITEM = 1;
        
        
        int LIST_ITEM = 2;

        
        int compareTo(Item item);

        
        int getType();

        
        boolean isNull();
    }

    
    private static class IntegerItem implements Item {
        
        
        private static final BigInteger BIG_INTEGER_ZERO = new BigInteger("0");

        
        private final BigInteger value;

        
        public static final IntegerItem ZERO = new IntegerItem();

        
        private IntegerItem() {
            this.value = BIG_INTEGER_ZERO;
        }

        
        public IntegerItem(String str) {
            this.value = new BigInteger(str);
        }

        
        public int getType() {
            return INTEGER_ITEM;
        }

        
        public boolean isNull() {
            return BIG_INTEGER_ZERO.equals(value);
        }

        
        public int compareTo(Item item) {
            if (item == null) {
                return BIG_INTEGER_ZERO.equals(value) ? 0 : 1; 
                                                               
            }

            switch (item.getType()) {
            case INTEGER_ITEM:
                return value.compareTo(((IntegerItem) item).value);

            case STRING_ITEM:
                return 1; 

            case LIST_ITEM:
                return 1; 

            default:
                throw new RuntimeException("invalid item: " + item.getClass());
            }
        }

        
        public String toString() {
            return value.toString();
        }
    }

    
    private static class StringItem implements Item {
        
        
        private static final String[] QUALIFIERS = { "alpha", "beta", "milestone", "rc", "snapshot", "", "sp" };

        
        private static final List<String> AR_QUALIFIERS = Arrays.asList(QUALIFIERS);

        
        private static final Properties ALIASES = new Properties();

        static {
            ALIASES.put("ga", "");
            ALIASES.put("final", "");
            ALIASES.put("cr", "rc");
        }

        
        private static final String RELEASE_VERSION_INDEX = String.valueOf(AR_QUALIFIERS.indexOf(""));

        
        private String value;

        
        public StringItem(String value, boolean followedByDigit) {
            if (followedByDigit && value.length() == 1) {
                
                switch (value.charAt(0)) {
                case 'a':
                    value = "alpha";
                    break;
                case 'b':
                    value = "beta";
                    break;
                case 'm':
                    value = "milestone";
                    break;
                default:
                }
            }
            this.value = ALIASES.getProperty(value, value);
        }

        
        public int getType() {
            return STRING_ITEM;
        }

        
        public boolean isNull() {
            return (comparableQualifier(value).compareTo(RELEASE_VERSION_INDEX) == 0);
        }

        
        public static String comparableQualifier(String qualifier) {
            int i = AR_QUALIFIERS.indexOf(qualifier);

            return i == -1 ? (AR_QUALIFIERS.size() + "-" + qualifier) : String.valueOf(i);
        }

        
        public int compareTo(Item item) {
            if (item == null) {
                
                return comparableQualifier(value).compareTo(RELEASE_VERSION_INDEX);
            }
            switch (item.getType()) {
            case INTEGER_ITEM:
                return -1; 

            case STRING_ITEM:
                return comparableQualifier(value).compareTo(comparableQualifier(((StringItem) item).value));

            case LIST_ITEM:
                return -1; 

            default:
                throw new RuntimeException("invalid item: " + item.getClass());
            }
        }

        
        public String toString() {
            return value;
        }
    }

    
    private static class ListItem extends ArrayList<Item>implements Item {
        
        
        private static final long serialVersionUID = 1L;

        
        public int getType() {
            return LIST_ITEM;
        }

        
        public boolean isNull() {
            return (size() == 0);
        }

        
        void normalize() {
            for (int i = size() - 1; i >= 0; i--) {
                Item lastItem = get(i);

                if (lastItem.isNull()) {
                    
                    remove(i);
                } else if (!(lastItem instanceof ListItem)) {
                    break;
                }
            }
        }

        
        public int compareTo(Item item) {
            if (item == null) {
                if (size() == 0) {
                    return 0; 
                }
                Item first = get(0);
                return first.compareTo(null);
            }
            switch (item.getType()) {
            case INTEGER_ITEM:
                return -1; 

            case STRING_ITEM:
                return 1; 

            case LIST_ITEM:
                Iterator<Item> left = iterator();
                Iterator<Item> right = ((ListItem) item).iterator();

                while (left.hasNext() || right.hasNext()) {
                    Item l = left.hasNext() ? left.next() : null;
                    Item r = right.hasNext() ? right.next() : null;

                    
                    
                    int result = l == null ? (r == null ? 0 : -1 * r.compareTo(l)) : l.compareTo(r);

                    if (result != 0) {
                        return result;
                    }
                }

                return 0;

            default:
                throw new RuntimeException("invalid item: " + item.getClass());
            }
        }

        
        public String toString() {
            StringBuilder buffer = new StringBuilder();
            for (Item item : this) {
                if (buffer.length() > 0) {
                    buffer.append((item instanceof ListItem) ? '-' : '.');
                }
                buffer.append(item);
            }
            return buffer.toString();
        }
    }

    
    public ComparableVersion(String version) {
        parseVersion(version);
    }

    
    public final void parseVersion(String version) {
        this.value = version;

        items = new ListItem();

        version = version.toLowerCase(Locale.ENGLISH);

        ListItem list = items;

        Stack<Item> stack = new Stack<>();
        stack.push(list);

        boolean isDigit = false;

        int startIndex = 0;

        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);

            if (c == '.') {
                if (i == startIndex) {
                    list.add(IntegerItem.ZERO);
                } else {
                    list.add(parseItem(isDigit, version.substring(startIndex, i)));
                }
                startIndex = i + 1;
            } else if (c == '-') {
                if (i == startIndex) {
                    list.add(IntegerItem.ZERO);
                } else {
                    list.add(parseItem(isDigit, version.substring(startIndex, i)));
                }
                startIndex = i + 1;

                list.add(list = new ListItem());
                stack.push(list);
            } else if (Character.isDigit(c)) {
                if (!isDigit && i > startIndex) {
                    list.add(new StringItem(version.substring(startIndex, i), true));
                    startIndex = i;

                    list.add(list = new ListItem());
                    stack.push(list);
                }

                isDigit = true;
            } else {
                if (isDigit && i > startIndex) {
                    list.add(parseItem(true, version.substring(startIndex, i)));
                    startIndex = i;

                    list.add(list = new ListItem());
                    stack.push(list);
                }

                isDigit = false;
            }
        }

        if (version.length() > startIndex) {
            list.add(parseItem(isDigit, version.substring(startIndex)));
        }

        while (!stack.isEmpty()) {
            list = (ListItem) stack.pop();
            list.normalize();
        }

        canonical = items.toString();
    }

    
    private static Item parseItem(boolean isDigit, String buf) {
        return isDigit ? new IntegerItem(buf) : new StringItem(buf, false);
    }

    
    public int compareTo(ComparableVersion o) {
        return items.compareTo(o.items);
    }

    
    public String toString() {
        return value;
    }

    
    public String getCanonical() {
        return canonical;
    }

    
    public boolean equals(Object o) {
        return (o instanceof ComparableVersion) && canonical.equals(ComparableVersion.class.cast(o).canonical);
    }

    
    public int hashCode() {
        return canonical.hashCode();
    }

    
    public static void main(String... args) {
        System.out.println("Display parameters as parsed by Maven (in canonical form) and comparison result:");
        if (args.length == 0) {
            return;
        }

        ComparableVersion prev = null;
        int i = 1;
        for (String version : args) {
            ComparableVersion c = new ComparableVersion(version);

            if (prev != null) {
                int compare = prev.compareTo(c);
                System.out.println("   " + prev.toString() + ' ' + ((compare == 0) ? "==" : ((compare < 0) ? "<" : ">")) + ' ' + version);
            }

            System.out.println(String.valueOf(i++) + ". " + version + " == " + c.getCanonical());

            prev = c;
        }
    }
}
