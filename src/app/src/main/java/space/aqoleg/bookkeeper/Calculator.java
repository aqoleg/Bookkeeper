// Calculator
package space.aqoleg.bookkeeper;

import java.math.BigDecimal;

class Calculator {
    // Local currency, if has, else main currency
    // result = start + delta, if forward
    // result = start - delta, if backward
    // For local currency:
    // resultMain = result * course
    // resultMain = startMain + deltaMain
    private BigDecimal start;
    private BigDecimal result;
    private BigDecimal startMain;
    private BigDecimal resultMain;
    private BigDecimal course;

    private Calculator(BigDecimal start, BigDecimal startMain, BigDecimal course) {
        this.start = start;
        this.startMain = startMain;
        this.course = course;
    }

    // Return 1 / input
    static String invert(String input) {
        BigDecimal bd = new BigDecimal(input);
        if (bd.signum() != 1) {
            return "0";
        }
        bd = BigDecimal.ONE.divide(bd, 8, BigDecimal.ROUND_HALF_EVEN);
        if (bd.signum() == 0) {
            return "0";
        } else {
            return bd.stripTrailingZeros().toPlainString();
        }
    }

    // Return input for course
    static String inputCourse(String input) {
        BigDecimal bd = new BigDecimal(input);
        if (bd.signum() != 1) {
            return "0.00000001";
        }
        // Strip zeros only in front of number
        int scale = bd.scale();
        return bd.stripTrailingZeros().setScale(scale, BigDecimal.ROUND_HALF_EVEN).toPlainString();
    }

    // Return input for transaction
    static String inputTransaction(String input) {
        BigDecimal bd = new BigDecimal(input);
        if (bd.signum() != 1) {
            return "0";
        }
        return bd.stripTrailingZeros().toPlainString();
    }

    static Calculator get(String start, String startMain, String course) {
        BigDecimal startBd = start == null ? BigDecimal.ZERO : new BigDecimal(start); // zero for add(value)
        BigDecimal startMainBd = startMain == null ? null : new BigDecimal(startMain);
        BigDecimal courseBd = null;
        if (course != null) {
            courseBd = new BigDecimal(course);
            if (courseBd.signum() != 1) {
                throw new IllegalArgumentException("Non-positive course");
            }
        }
        return new Calculator(startBd, startMainBd, courseBd);
    }

    // start += start
    void add(String value) {
        start = start.add(new BigDecimal(value));
    }

    // Return result of add(value)
    String getTotal() {
        if (start.signum() == 0) {
            return "0";
        } else {
            return start.stripTrailingZeros().toPlainString();
        }
    }

    // Return delta = result - start
    String getDeltaByResult(String result) {
        this.result = new BigDecimal(result);
        BigDecimal delta = this.result.subtract(start);
        if (delta.signum() == 0) {
            return "0";
        } else {
            return delta.stripTrailingZeros().toPlainString();
        }
    }

    // If forward return result = start + delta
    // else return result = start - delta
    String getResultByDelta(String delta, boolean forward) {
        if (forward) {
            result = start.add(new BigDecimal(delta));
        } else {
            result = start.subtract(new BigDecimal(delta));
        }
        if (result.signum() == 0) {
            return "0";
        } else {
            return result.stripTrailingZeros().toPlainString();
        }
    }

    // Return deltaMain = resultMain - startMain
    String getDeltaMain() {
        BigDecimal delta = resultMain.subtract(startMain);
        if (delta.signum() == 0) {
            return "0";
        } else {
            return delta.stripTrailingZeros().toPlainString();
        }
    }

    // Return resultMain = result * course
    String getResultMain(String result) {
        if (result != null) {
            this.result = new BigDecimal(result);
        }
        resultMain = this.result
                .multiply(course)
                .setScale(course.scale(), BigDecimal.ROUND_HALF_EVEN);
        if (resultMain.signum() == 0) {
            return "0";
        } else {
            return resultMain.stripTrailingZeros().toPlainString();
        }
    }
}