const DAY_MILLIS: i64 = 86_400_000;
const RUNEDAY_OFFSET: i32 = 11_745;
const UNIX_FIXED: i64 = 719_163;
const CUTOVER_FIXED: i64 = 577_736;
const CUTOVER_YEAR: i32 = 1582;
const CUMULATIVE: [i64; 13] = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365];

pub(crate) fn is_leap_year(year: i32) -> bool {
    if year < 0 {
        (year + 1) % 4 == 0
    } else if year < CUTOVER_YEAR {
        year % 4 == 0
    } else if year % 4 != 0 {
        false
    } else {
        year % 100 != 0 || year % 400 == 0
    }
}

pub(crate) fn minutes_from_runeday(days: i32) -> i32 {
    ((i64::from(days.wrapping_add(RUNEDAY_OFFSET)) * DAY_MILLIS) / 60_000) as i32
}

pub(crate) fn runeday_from_date(day: i32, month: i32, year: i32) -> i32 {
    let millis = gmt_noon(day, month, year);
    let mut runeday = (millis / DAY_MILLIS - i64::from(RUNEDAY_OFFSET)) as i32;
    if year < 1970 {
        runeday = runeday.wrapping_sub(1);
    }
    runeday
}

fn floor_div(value: i64, divisor: i64) -> i64 {
    value.div_euclid(divisor)
}

fn gregorian_leap(year: i64) -> bool {
    year.rem_euclid(4) == 0 && (year.rem_euclid(100) != 0 || year.rem_euclid(400) == 0)
}

fn julian_leap(year: i64) -> bool {
    year.rem_euclid(4) == 0
}

fn month_start(leap: bool, month: i64) -> i64 {
    CUMULATIVE[month as usize] + i64::from(leap && month >= 2)
}

fn gregorian_first(year: i64, month: i64) -> i64 {
    let previous = year.wrapping_sub(1);
    365i64
        .wrapping_mul(previous)
        .wrapping_add(floor_div(previous, 4))
        .wrapping_sub(floor_div(previous, 100))
        .wrapping_add(floor_div(previous, 400))
        .wrapping_add(month_start(gregorian_leap(year), month))
        .wrapping_add(1)
}

fn julian_first(year: i64, month: i64) -> i64 {
    let previous = year.wrapping_sub(1);
    365i64
        .wrapping_mul(previous)
        .wrapping_add(floor_div(previous, 4))
        .wrapping_add(month_start(julian_leap(year), month))
        .wrapping_sub(1)
}

fn gmt_noon(day: i32, month: i32, year: i32) -> i64 {
    let original = year;
    let mut year = year;
    let mut month = month;
    if month > 11 {
        year = year.wrapping_add(month / 12);
        month %= 12;
    } else if month < 0 {
        year = year.wrapping_add(month.div_euclid(12));
        month = month.rem_euclid(12);
    }
    let first = if original < CUTOVER_YEAR {
        julian_first(i64::from(year), i64::from(month))
    } else {
        let gregorian = gregorian_first(i64::from(year), i64::from(month));
        if gregorian.wrapping_add(i64::from(day) - 1) < CUTOVER_FIXED {
            julian_first(i64::from(year), i64::from(month))
        } else {
            gregorian
        }
    };
    let fixed = first.wrapping_add(i64::from(day) - 1);
    fixed
        .wrapping_sub(UNIX_FIXED)
        .wrapping_mul(DAY_MILLIS)
        .wrapping_add(12 * 3_600_000)
}

pub(crate) struct Fields {
    pub day: i32,
    pub month: i32,
    pub year: i32,
    pub hour: i32,
    pub minute: i32,
}

/// Calendar fields of a local wall-clock instant, with the Julian to Gregorian cutover of
/// `java.util.GregorianCalendar` and era years for dates before year 1.
pub(crate) fn fields(local_millis: i64) -> Fields {
    let fixed = floor_div(local_millis, DAY_MILLIS).wrapping_add(UNIX_FIXED);
    let millis_of_day = local_millis.rem_euclid(DAY_MILLIS);
    let gregorian = fixed >= CUTOVER_FIXED;
    let first: fn(i64, i64) -> i64 = if gregorian { gregorian_first } else { julian_first };
    let mut year = if gregorian {
        floor_div(fixed.wrapping_mul(400), 146_097) + 1
    } else {
        floor_div(fixed.wrapping_mul(4), 1461) + 1
    };
    while first(year, 0) > fixed {
        year -= 1;
    }
    while first(year + 1, 0) <= fixed {
        year += 1;
    }
    let mut month = 11;
    while first(year, month) > fixed {
        month -= 1;
    }
    let day = fixed - first(year, month) + 1;
    let year = if !gregorian && year <= 0 { 1 - year } else { year };
    Fields {
        day: day as i32,
        month: month as i32,
        year: year as i32,
        hour: (millis_of_day / 3_600_000) as i32,
        minute: (millis_of_day / 60_000 % 60) as i32,
    }
}

pub(crate) const MONTHS: [[&str; 12]; 5] = [
    ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"],
    ["Jan", "Feb", "M\u{e4}r", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez"],
    ["jan", "f\u{e9}v", "mars", "avr", "mai", "juin", "juil", "ao\u{fb}t", "sept", "oct", "nov", "d\u{e9}c"],
    ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"],
    ["jan", "feb", "mrt", "apr", "mei", "jun", "jul", "aug", "sep", "okt", "nov", "dec"],
];

fn two(value: i32) -> String {
    format!("{}{}", value / 10, value % 10)
}

/// `dd/mm/yy`, as `CalendarUtil.getLocalTimestring`.
pub(crate) fn slash_date(fields: &Fields) -> String {
    format!(
        "{}/{}/{}",
        two(fields.day),
        two(fields.month + 1),
        two(fields.year % 100)
    )
}

/// `dd-Mon-year`, or `None` when the language has no month table (a Java array index error).
pub(crate) fn dash_date(fields: &Fields, language: i32) -> Option<String> {
    let months = usize::try_from(language).ok().and_then(|index| MONTHS.get(index))?;
    Some(format!(
        "{}-{}-{}",
        two(fields.day),
        months[fields.month as usize],
        fields.year
    ))
}

pub(crate) fn time_of(fields: &Fields) -> String {
    format!("{}:{}", two(fields.hour), two(fields.minute))
}
