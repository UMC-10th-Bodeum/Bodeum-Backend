package com.bodeum.domain.user.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GuardianLevelTest {

    @Test
    void zeroPointStartsAtSproutLevelOne() {
        GuardianLevel level = GuardianLevel.from(0);

        assertThat(level).isEqualTo(GuardianLevel.SPROUT);
        assertThat(level.getLevelNumber()).isEqualTo(1);
        assertThat(level.getBadgeName()).isEqualTo("새싹");
    }

    @ParameterizedTest
    @CsvSource({
            "0, SPROUT",
            "49, SPROUT",
            "50, LEAF",
            "199, LEAF",
            "200, FLOWER",
            "499, FLOWER",
            "500, FRUIT",
            "999, FRUIT",
            "1000, TREE",
            "100000, TREE"
    })
    void mapsPointToLevelAtBoundaries(int point, GuardianLevel expected) {
        assertThat(GuardianLevel.from(point)).isEqualTo(expected);
    }
}
