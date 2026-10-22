use openrs_protocol::Feed;
use openrs_protocol::js5::{self, HandshakeDecoder, HandshakeReply, Priority, Reply, ReplyDecoder};

pub(crate) struct Transfer {
    handshake: HandshakeDecoder,
    replies: ReplyDecoder,
    outgoing: Vec<u8>,
    accepted: bool,
    pending: Option<(u8, u32)>,
    prefetch_count: usize,
}

impl Transfer {
    pub(crate) fn new(token: &[u8], max_container_bytes: usize) -> Result<Self, String> {
        Ok(Self {
            handshake: HandshakeDecoder::new(js5::REVISION727_PREFETCH_COUNT)
                .map_err(|error| error.to_string())?,
            replies: ReplyDecoder::new(max_container_bytes).map_err(|error| error.to_string())?,
            outgoing: js5::encode_handshake(token).map_err(|error| error.to_string())?,
            accepted: false,
            pending: Some((js5::VERSION_TABLE_INDEX, js5::VERSION_TABLE_ARCHIVE)),
            prefetch_count: 0,
        })
    }

    pub(crate) fn outgoing(&mut self) -> Vec<u8> {
        std::mem::take(&mut self.outgoing)
    }

    pub(crate) fn prefetch_count(&self) -> usize {
        self.prefetch_count
    }

    pub(crate) fn request(&mut self, index: u8, archive: u32) -> Result<(), String> {
        if !self.accepted || self.pending.is_some() {
            return Err("JS5 request already pending or handshake incomplete".into());
        }
        self.pending = Some((index, archive));
        self.outgoing
            .extend_from_slice(&js5::encode_request(index, archive, Priority::High));
        Ok(())
    }

    pub(crate) fn feed(&mut self, bytes: &[u8]) -> Result<Feed<Reply>, String> {
        if !self.accepted {
            let feed = self
                .handshake
                .feed(bytes)
                .map_err(|error| error.to_string())?;
            if let Some(reply) = feed.item {
                match reply {
                    HandshakeReply::Accepted { prefetch_keys } => {
                        self.accepted = true;
                        self.prefetch_count = prefetch_keys.len();
                        self.outgoing.extend_from_slice(&js5::acknowledge());
                        self.outgoing.extend_from_slice(&js5::encode_status(false));
                        let (index, archive) = self
                            .pending
                            .ok_or_else(|| "JS5 initial request missing".to_string())?;
                        self.outgoing.extend_from_slice(&js5::encode_request(
                            index,
                            archive,
                            Priority::High,
                        ));
                    }
                    HandshakeReply::Rejected { status } => {
                        return Err(format!("JS5 handshake rejected: {status}"));
                    }
                }
            }
            return Ok(Feed {
                consumed: feed.consumed,
                item: None,
            });
        }
        let expected = self
            .pending
            .ok_or_else(|| "unexpected bytes without a JS5 request".to_string())?;
        let feed = self
            .replies
            .feed(bytes)
            .map_err(|error| error.to_string())?;
        if let Some(reply) = &feed.item {
            if (reply.index, reply.archive) != expected || reply.priority != Priority::High {
                return Err("unexpected JS5 reply identity".into());
            }
            self.pending = None;
        }
        Ok(feed)
    }
}
