import typing
import json
import logic
import duelists
import cards
from duelists import Duelist
from logic import OptionsProxy, LogicCard
from utils import Constants, flatten
from cards import Card
from drop_pools import Drop
from collections import Counter
from version import __version__


duelist_unlock_order: typing.Tuple[typing.Tuple[Duelist, ...]] = tuple()
final_6_order: typing.Tuple[Duelist] = tuple()
options: typing.Optional[OptionsProxy] = None


def get_version() -> str:
    return __version__


def initialize(slot_data_raw: str) -> None:
    global duelist_unlock_order, final_6_order, options
    slot_data: typing.Dict = json.loads(slot_data_raw)
    duelist_unlock_order = duelists.map_ids_to_duelists(slot_data[Constants.DUELIST_UNLOCK_ORDER_KEY])
    final_6_order = tuple([duelists.ids_to_duelists[i] for i in slot_data[Constants.FINAL_6_ORDER_KEY]])
    options = OptionsProxy(slot_data[Constants.GAME_OPTIONS_KEY])


def get_total_cards_per_farm() -> typing.Dict[typing.Tuple[Duelist, str], int]:
    # (duelist, duel_rank) -> total_card_locations
    all_drops_with_card_locations: typing.List[Drop] = flatten([
        c.drop_pool for c in cards.all_cards if c.drop_pool is not None
    ])
    totals: typing.Counter[typing.Tuple[str, str]] = Counter()
    for drop in all_drops_with_card_locations:
        key: typing.Tuple[Duelist, str] = (drop.duelist, str(drop.duel_rank.name))
        totals[key] += 1
    return dict(totals)


def get_tracker_info(
        items_received_ids: typing.Sequence[int],
        missing_location_ids: typing.Sequence[int]
) -> typing.Dict[str, typing.Any]:
    progressive_duelist_item_count: int = sum(1 for i in items_received_ids if i == Constants.PROGRESSIVE_DUELIST_ITEM_ID)
    unlocked_duelists: typing.List[Duelist] = [d for d in logic.get_unlocked_duelists(
        progressive_duelist_item_count, duelist_unlock_order, final_6_order
    )]

    all_cards_with_items: typing.List[Card] = logic.get_all_cards_that_have_locations(options)
    missing_cards: typing.Dict[int, Card] = {card.id: card for card in all_cards_with_items if card.location_id in missing_location_ids}
    # TODO (maybe?) pass in-logic droppools to Tracker instead of filtering them
    non_excluded_cards: typing.List[LogicCard] = logic.filter_to_in_logic_cards(missing_cards.values(), options)
    duelists_to_pools = {d: [] for d in unlocked_duelists}
    # (card_name, duel_rank, probability, in_logic: bool)
    for logic_card in non_excluded_cards:
        drop_pools: typing.Tuple[Drop, ...] = logic_card.card.drop_pool
        for drop in drop_pools:
            if drop.duelist in duelists_to_pools:
                duelists_to_pools[drop.duelist].append((
                    logic_card.card.name,
                    str(drop.duel_rank.name),
                    drop.probability,
                    drop.duelist in [accessible_drop.duelist for accessible_drop in logic_card.accessible_drops]
                ))

    return {(key.id, str(key)): value for key, value in duelists_to_pools.items()}
