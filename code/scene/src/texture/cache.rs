pub struct RowCache {
    capacity: usize,
    rows: usize,
    entries: Vec<Option<usize>>,
    recent: Vec<usize>,
    allocated: usize,
    last_row: i32,
}

impl RowCache {
    pub fn new(capacity: usize, rows: usize) -> Self {
        RowCache { capacity, rows, entries: vec![None; rows], recent: Vec::new(), allocated: 0, last_row: -1 }
    }

    pub fn is_full(&self) -> bool {
        self.capacity == self.rows
    }

    pub fn slot_count(&self) -> usize {
        self.capacity
    }

    pub fn get(&mut self, row: usize) -> (usize, bool) {
        if self.is_full() {
            let dirty = self.entries[row].is_none();
            self.entries[row] = Some(row);
            return (row, dirty);
        }
        if self.capacity == 1 {
            let dirty = self.last_row != row as i32;
            self.last_row = row as i32;
            return (0, dirty);
        }
        if let Some(slot) = self.entries[row] {
            self.recent.retain(|r| *r != row);
            self.recent.insert(0, row);
            return (slot, false);
        }
        let slot = if self.allocated >= self.capacity {
            let oldest = self.recent.pop().expect("cache list holds every allocated slot");
            self.entries[oldest].take().expect("evicted row holds a slot")
        } else {
            self.allocated += 1;
            self.allocated - 1
        };
        self.entries[row] = Some(slot);
        self.recent.insert(0, row);
        (slot, true)
    }

    pub fn mark_all(&mut self) {
        assert!(self.is_full(), "whole-image access needs a full cache");
        for entry in self.entries.iter_mut().enumerate() {
            *entry.1 = Some(entry.0);
        }
    }
}
