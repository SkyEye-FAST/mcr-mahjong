// Test-only adapter to the pinned MIT-licensed mahjong-algorithm sources.
// Copyright (c) 2026 SkyEye_FAST. MIT; see ../LICENSE and ../NOTICE.
#include "fan_calculator.h"
#include "shanten.h"
#include "stringify.h"
#include <iostream>
#include <sstream>
#include <string>
#include <vector>
#include <limits>

using namespace mahjong;

static std::string useful_bits(const useful_table_t &table) {
    std::string bits;
    for (tile_t tile : standard_tiles<>::all) bits += table[tile] ? '1' : '0';
    return bits;
}

int main() {
    std::string line;
    while (std::getline(std::cin, line)) {
        // Windows PowerShell's redirected StreamWriter may prepend a UTF-8 BOM.
        if (line.compare(0, 3, "\xEF\xBB\xBF") == 0) line.erase(0, 3);
        std::istringstream in(line);
        std::vector<std::string> fields;
        std::string field;
        while (std::getline(in, field, '|')) fields.push_back(field);
        if (fields.size() < 2) return 2;
        hand_tiles_t hand{};
        tile_t drawn = 0;
        int parsed = parse_hand_tiles(fields[1].data(), fields[1].size(), &hand, &drawn);
        if (parsed != 0) { std::cout << "PARSE:" << parsed << std::endl; continue; }
        if (fields[0] == "F" && fields.size() == 6) {
            calculate_param_t param{};
            param.hand_tiles = hand;
            param.win_tile = drawn;
            param.win_flag = static_cast<win_flag_t>(std::stoi(fields[2]));
            param.prevalent_wind = static_cast<wind_t>(std::stoi(fields[3]));
            param.seat_wind = static_cast<wind_t>(std::stoi(fields[4]));
            param.flower_count = static_cast<uint8_t>(std::stoi(fields[5]));
            fan_table_t table{};
            const int total = calculate_fan(&param, &table);
            std::cout << total;
            for (int i = 1; i < FAN_TABLE_SIZE; ++i) std::cout << ',' << table[i];
            std::cout << std::endl;
        } else if (fields[0] == "S") {
            using function = int (*)(const tile_t *, intptr_t, useful_table_t *);
            function forms[] = {regular_shanten, seven_pairs_shanten, thirteen_orphans_shanten,
                honors_and_knitted_tiles_shanten, knitted_straight_shanten};
            for (int i = 0; i < 5; ++i) {
                useful_table_t useful{};
                // Unsupported forms return INT_MAX; their output table is unspecified.
                // In particular, the upstream honors wrapper reads an uninitialized
                // temporary on such inputs, so never call it with the wrong tile count.
                const bool applicable = i == 0 || hand.tile_count == 13 || (i == 4 && hand.tile_count == 10);
                int shanten = applicable ? forms[i](hand.standing_tiles, hand.tile_count, &useful)
                                         : std::numeric_limits<int>::max();
                if (i) std::cout << ';';
                std::cout << shanten << ',' << useful_bits(useful);
            }
            useful_table_t waiting{};
            is_waiting(hand, &waiting);
            std::cout << ';' << useful_bits(waiting) << std::endl;
        } else if (fields[0] == "D") {
            bool first = true;
            enum_discard_tile(&hand, drawn, 31, &first, [](void *context, const enum_result_t *result) {
                bool &first = *static_cast<bool *>(context);
                if (!first) std::cout << ';';
                first = false;
                std::cout << static_cast<int>(result->discard_tile) << ',' << static_cast<int>(result->form_flag)
                          << ',' << result->shanten << ',' << useful_bits(result->useful_table);
                return true;
            });
            std::cout << std::endl;
        } else return 3;
    }
}
