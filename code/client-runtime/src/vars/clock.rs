use super::WorldVarHost;
use crate::environment::Environment;
use openrs_cs2::HostError;
use std::sync::Arc;

#[derive(Default)]
pub(super) struct ClockBinding<'a> {
    environment: Option<&'a mut Environment>,
    elapsed_ms: u64,
    table: Option<Arc<[i32]>>,
}

impl ClockBinding<'_> {
    pub(super) fn clock_millis(&self) -> Result<i64, HostError> {
        self.environment
            .as_deref()
            .and_then(|environment| environment.epoch_ms(self.elapsed_ms))
            .ok_or(HostError::Unavailable)
    }

    pub(super) fn random_unit(&mut self) -> Result<f64, HostError> {
        self.environment
            .as_deref_mut()
            .map(Environment::draw)
            .ok_or(HostError::Unavailable)
    }

    pub(super) fn local_offset_millis(&self) -> Result<i64, HostError> {
        self.environment
            .as_deref()
            .map(Environment::offset_ms)
            .ok_or(HostError::Unavailable)
    }

    pub(super) fn language(&self) -> Result<Option<i32>, HostError> {
        self.environment
            .as_deref()
            .map(Environment::language)
            .ok_or(HostError::Unavailable)
    }

    pub(super) fn hsv_to_rgb_table(&self) -> Result<Option<Arc<[i32]>>, HostError> {
        self.environment
            .as_deref()
            .map(|_| self.table.clone())
            .ok_or(HostError::Unavailable)
    }
}

impl<'a> WorldVarHost<'a> {
    pub fn with_environment(mut self, environment: &'a mut Environment, elapsed_ms: u64) -> Self {
        self.clock.table = Some(environment.hsv_table());
        self.clock.environment = Some(environment);
        self.clock.elapsed_ms = elapsed_ms;
        self
    }
}
