package ocean_block.system.stations.item

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class StationBlockIdsTest : StringSpec({

    "peneiras e fundicoes sao estacoes" {
        listOf("peneira", "peneira_ferro", "peneira_diamante", "peneira_automatica", "fundicao", "fundicao_melhorada", "fundicao_funil")
            .all(StationBlockIds::isStation) shouldBe true
    }

    "outros blocos nao sao estacoes" {
        StationBlockIds.isStation("rede") shouldBe false
        StationBlockIds.isStation("mesa_pesquisa") shouldBe false
        StationBlockIds.isStation(null) shouldBe false
    }
})
