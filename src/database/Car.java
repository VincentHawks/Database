package database;

import java.util.HashMap;

public class Car {

    private static HashMap<String, String> lookupTable; // cyrillic -> latin

    public String license;
    public String make;
    public String model;
    public int year;
    public boolean automatic;
    public double volume;

    public static void initLookupTable() {
        // Init lookup table
        // Alphabet: А В Е К М Н О Р С Т У Х
        lookupTable = new HashMap<>();
        lookupTable.put("А", "A");
        lookupTable.put("В", "B");
        lookupTable.put("Е", "E");
        lookupTable.put("К", "K");
        lookupTable.put("М", "M");
        lookupTable.put("Н", "H");
        lookupTable.put("О", "O");
        lookupTable.put("Р", "P");
        lookupTable.put("С", "C");
        lookupTable.put("Т", "T");
        lookupTable.put("У", "Y");
        lookupTable.put("Х", "X");
    }

    public static String conformLicense(String license) {
        license = license.toUpperCase();
        StringBuilder conformedLicense = new StringBuilder();
        for(String c : license.split("")) {
            if(Character.isDigit(c.charAt(0))) {
                conformedLicense.append(c);
            }
            else if(lookupTable.containsKey(c)) {
                conformedLicense.append(lookupTable.get(c));
            }
            else {
                conformedLicense.append(c);
            }
        }
        return conformedLicense.toString();
    }

    public Car(String license, String make, String model, int year, boolean automatic, double volume) {
        this.license = conformLicense(license);
        this.make = make;
        this.model = model;
        this.year = year;
        this.automatic = automatic;
        this.volume = volume;
    }

    public static Car parseCar(String car) {
        String[] tokens = car.split("`");
        return new Car(
          tokens[0], // license
          tokens[1], // make
          tokens[2], // model
          Integer.parseInt(tokens[3]), // year
          Boolean.parseBoolean(tokens[4]), // automatic
          Double.parseDouble(tokens[5]) // volume
        );
    }

    @Override
    public String toString() {
        return new StringBuilder().append(license)
                .append('`') // ` (backtick) is really unlikely to be used in any relevant string, which makes it a perfect separator
                .append(make)
                .append('`')
                .append(model)
                .append('`')
                .append(year)
                .append('`')
                .append(automatic)
                .append('`')
                .append(volume)
                .toString();

    }

}
