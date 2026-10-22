use crate::framing::{OpcodeEncoding, PacketDecoder, PacketEncoder, PacketSize};
use crate::isaac::Isaac;
use crate::{Error, Feed};
use std::{fmt::Debug, marker::PhantomData};

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum Direction {
    ClientToServer,
    ServerToClient,
}

impl Direction {
    fn opcode_encoding(self) -> OpcodeEncoding {
        match self {
            Self::ClientToServer => OpcodeEncoding::Byte,
            Self::ServerToClient => OpcodeEncoding::Smart,
        }
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum PacketIdentity {
    Named,
    UnsupportedUnknown,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct PacketMetadata<O> {
    pub packet: O,
    pub name: &'static str,
    pub native_size: i32,
    pub size: PacketSize,
    pub identity: PacketIdentity,
    pub tick_processed: Option<bool>,
}

mod sealed {
    pub trait Sealed {}
}

pub trait GameOpcode: sealed::Sealed + Copy + Debug + Eq + 'static {
    const DIRECTION: Direction;

    fn from_wire(wire: u16) -> Option<Self>;
    fn wire(self) -> u16;
    fn metadata(self) -> &'static PacketMetadata<Self>;
    fn sizes() -> &'static [Option<PacketSize>];
}

include!(concat!(env!("OUT_DIR"), "/game_packets.rs"));

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct GamePacket<O> {
    pub opcode: O,
    pub payload: Vec<u8>,
}

pub struct RegistryEncoder<O> {
    inner: PacketEncoder,
    direction: PhantomData<O>,
}

impl<O: GameOpcode> RegistryEncoder<O> {
    pub fn new(cipher: Option<Isaac>, max_payload_bytes: usize) -> Result<Self, Error> {
        Ok(Self {
            inner: PacketEncoder::new(cipher, max_payload_bytes)?
                .with_opcode_encoding(O::DIRECTION.opcode_encoding()),
            direction: PhantomData,
        })
    }

    pub fn encode(&mut self, opcode: O, payload: &[u8]) -> Result<Vec<u8>, Error> {
        self.inner
            .encode(opcode.wire(), opcode.metadata().size, payload)
    }
}

pub struct RegistryDecoder<O> {
    inner: PacketDecoder<'static>,
    direction: PhantomData<O>,
}

impl<O: GameOpcode> RegistryDecoder<O> {
    pub fn new(cipher: Option<Isaac>, max_payload_bytes: usize) -> Result<Self, Error> {
        Ok(Self {
            inner: PacketDecoder::new(cipher, O::sizes(), max_payload_bytes)?
                .with_opcode_encoding(O::DIRECTION.opcode_encoding()),
            direction: PhantomData,
        })
    }

    pub fn feed(&mut self, input: &[u8]) -> Result<Feed<GamePacket<O>>, Error> {
        let result = self.inner.feed(input)?;
        let item = result
            .item
            .map(|packet| {
                Ok(GamePacket {
                    opcode: O::from_wire(packet.opcode)
                        .ok_or(Error::UnknownOpcode(packet.opcode))?,
                    payload: packet.payload,
                })
            })
            .transpose()?;
        Ok(Feed {
            consumed: result.consumed,
            item,
        })
    }

    pub fn finish(&mut self) -> Result<(), Error> {
        self.inner.finish()
    }
}
