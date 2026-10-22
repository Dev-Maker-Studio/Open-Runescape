use crate::{Opcode, StackKind, VmErrorKind};

const INVERSE_POWER_ROUND_DOWN: &[(i32, i32)] = include!("inverse_power_round_down.rs");

pub(crate) fn execute(opcode: Opcode, stack: &mut Vec<i32>) -> Result<(), VmErrorKind> {
    use Opcode::*;
    let count = match opcode {
        NOT | COORDX | COORDY | COORDZ => 1,
        ADD | SUB | MULTIPLY | DIVIDE | ADDPERCENT | SETBIT | CLEARBIT | TESTBIT | MODULO | POW
        | INVPOW | AND | OR | MIN | MAX => 2,
        SCALE => 3,
        MOVECOORD => 4,
        INTERPOLATE => 5,
        _ => return Err(VmErrorKind::UnsupportedOpcode(opcode)),
    };
    let start = stack
        .len()
        .checked_sub(count)
        .ok_or(VmErrorKind::StackUnderflow(StackKind::Int))?;
    let values = &stack[start..];
    let result = match opcode {
        ADD => Ok(values[0].wrapping_add(values[1])),
        SUB => Ok(values[0].wrapping_sub(values[1])),
        MULTIPLY => Ok(values[0].wrapping_mul(values[1])),
        DIVIDE => divide(values[0], values[1]),
        MODULO if values[1] == 0 => Err(VmErrorKind::InvalidOperand),
        MODULO => Ok(values[0].wrapping_rem(values[1])),
        ADDPERCENT => {
            Ok((i64::from(values[0]) + i64::from(values[0]) * i64::from(values[1]) / 100) as i32)
        }
        SETBIT => Ok(values[0] | 1i32.wrapping_shl(values[1] as u32)),
        CLEARBIT => Ok(values[0] & !1i32.wrapping_shl(values[1] as u32)),
        TESTBIT => Ok(i32::from(
            values[0] & 1i32.wrapping_shl(values[1] as u32) != 0,
        )),
        POW => Ok(power(values[0], values[1])),
        INVPOW => Ok(inverse_power(values[0], values[1])),
        AND => Ok(values[0] & values[1]),
        OR => Ok(values[0] | values[1]),
        NOT => Ok(!values[0]),
        COORDX => Ok((values[0] >> 14) & 0x3fff),
        COORDY => Ok(values[0] >> 28),
        COORDZ => Ok(values[0] & 0x3fff),
        MOVECOORD => Ok(values[0]
            .wrapping_add(values[1].wrapping_shl(14))
            .wrapping_add(values[2].wrapping_shl(28))
            .wrapping_add(values[3])),
        MIN => Ok(values[0].min(values[1])),
        MAX => Ok(values[0].max(values[1])),
        SCALE if values[1] == 0 => Err(VmErrorKind::InvalidOperand),
        SCALE => Ok((i64::from(values[2]) * i64::from(values[0]) / i64::from(values[1])) as i32),
        INTERPOLATE => divide(
            values[4]
                .wrapping_sub(values[2])
                .wrapping_mul(values[1].wrapping_sub(values[0])),
            values[3].wrapping_sub(values[2]),
        )
        .map(|value| values[0].wrapping_add(value)),
        _ => return Err(VmErrorKind::UnsupportedOpcode(opcode)),
    };
    stack.truncate(start);
    stack.push(result?);
    Ok(())
}

fn inverse_power(base: i32, root: i32) -> i32 {
    if base == 0 {
        return 0;
    }
    if root == 0 {
        return i32::MAX;
    }
    if root == 1 {
        return base;
    }
    if base == 1 {
        return 1;
    }
    if base < 0 {
        return if base == -1 && root == -1 { -1 } else { 0 };
    }
    if root < 0 {
        return 0;
    }
    if root >= 31 {
        return 1;
    }
    let mut lower = 1;
    let mut upper = base.min(46341);
    while lower < upper {
        let middle = lower + (upper - lower + 1) / 2;
        if power_at_most(middle, root, base) {
            lower = middle;
        } else {
            upper = middle - 1;
        }
    }
    lower
        - i32::from(
            INVERSE_POWER_ROUND_DOWN
                .binary_search(&(root, base))
                .is_ok(),
        )
}

fn power_at_most(base: i32, exponent: i32, limit: i32) -> bool {
    let mut value = 1u64;
    for _ in 0..exponent {
        value *= base as u64;
        if value > limit as u64 {
            return false;
        }
    }
    true
}

fn divide(dividend: i32, divisor: i32) -> Result<i32, VmErrorKind> {
    if divisor == 0 {
        Err(VmErrorKind::InvalidOperand)
    } else {
        Ok(dividend.wrapping_div(divisor))
    }
}

fn power(base: i32, exponent: i32) -> i32 {
    if base == 0 {
        return 0;
    }
    let negative = base < 0 && exponent & 1 != 0;
    if base == 1 || base == -1 {
        return if negative { -1 } else { 1 };
    }
    if exponent < 0 {
        return 0;
    }
    let limit = if negative {
        -i64::from(i32::MIN)
    } else {
        i64::from(i32::MAX)
    };
    let factor = i64::from(base).abs();
    let mut value = 1i64;
    for _ in 0..exponent {
        value *= factor;
        if value > limit {
            return if negative { i32::MIN } else { i32::MAX };
        }
    }
    if negative {
        (-value) as i32
    } else {
        value as i32
    }
}
