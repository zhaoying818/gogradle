package com.github.blindpirate.gogradle.core

import org.junit.Test

import static com.github.blindpirate.gogradle.core.IncompleteGolangPackage.of

class IncompleteGolangPackageTest {

    @Test(expected = UnsupportedOperationException)
    void 'getVcsType should throw exception'() {
        of('incomplete').vcsType
    }

    @Test(expected = UnsupportedOperationException)
    void 'getUrl should throw exception'() {
        of('incomplete').urls
    }

    @Test(expected = UnsupportedOperationException)
    void 'getRootPath should throw exception'() {
        of('incomplete').rootPath
    }

    @Test
    void 'empty result should be returned when an incomplete package resolves a longer path'() {
        assert !of('incomplete/a').resolve('incomplete/a/b').isPresent()
        assert !of('incomplete/a').resolve('incomplete/a/b/c').isPresent()
    }

    @Test
    void 'shorter path of an incomplete path should also be incomplete'() {
        assert of('incomplete/a').resolve('incomplete').get() instanceof IncompleteGolangPackage
    }

}
