# Checagem de tipos da interface sem Android SDK

Ferramenta auxiliar usada quando o Google Maven / Android SDK não estão acessíveis.
Compila **todo o código Kotlin do app e dos testes de interface** contra o Compose
Multiplatform para desktop (mesmos pacotes `androidx.compose.*`, publicados no Maven Central),
usando *stubs* mínimos para as APIs exclusivas do Android (`stubs/`, `teststubs/`).

O que ela garante: as chamadas de Compose (ui, foundation, material3, ícones), navegação e
lifecycle-compose batem com as APIs reais dessas bibliotecas.

O que ela **não** garante: as chamadas ao Android SDK, AdMob, UMP, DataStore, androidx.lifecycle
e androidx.test são verificadas apenas contra os stubs, escritos a partir da documentação.
Isso não substitui `./gradlew :app:assembleDebug` num ambiente com o Android SDK.

    cd tools/ui-typecheck
    ../../gradlew compileKotlin compileTestKotlin
