package com.github.blindpirate.gogradle.core.dependency.resolve

import com.github.blindpirate.gogradle.GogradleRunner
import com.github.blindpirate.gogradle.WithResource
import com.github.blindpirate.gogradle.core.GolangPackageModule
import com.github.blindpirate.gogradle.core.dependency.GolangDependency
import com.github.blindpirate.gogradle.core.dependency.GolangDependencySet
import com.github.blindpirate.gogradle.core.dependency.parse.NotationParser
import com.github.blindpirate.gogradle.core.pack.PackageInfo
import com.github.blindpirate.gogradle.core.pack.PackageNameResolver
import com.github.blindpirate.gogradle.util.IOUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.when

@RunWith(GogradleRunner)
@WithResource('')
class SourceCodeDependencyFactoryTest {
    @Mock
    PackageNameResolver packageNameResolver
    @Mock
    NotationParser notationParser;

    SourceCodeDependencyFactory factory

    File resource

    @Mock
    GolangPackageModule module
    @Mock
    GolangDependency dependency

    @Before
    void setUp() {
        factory = new SourceCodeDependencyFactory(packageNameResolver, notationParser)
        when(module.getRootDir()).thenReturn(resource.toPath())
        when(notationParser.parse('github.com/a/b')).thenReturn(dependency)
        when(dependency.getName()).thenReturn('github.com/a/b')
        when(packageNameResolver.produce(anyString())).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                String name = invocation.getArgument(0);
                if (name.startsWith('github.com')) {
                    PackageInfo ret = PackageInfo.builder().withName(name).withRootName('github.com/a/b').build()
                    return Optional.of(ret)
                } else {
                    PackageInfo standardPackage = PackageInfo.standardPackage(name);
                    return Optional.of(standardPackage)
                }
            }
        })
    }

    @Test
    void 'empty dependency set should be produced when no go code exists'() {
        // when
        GolangDependencySet result = factory.produce(module).get()
        // then
        assert result.isEmpty()
    }

    @Test
    void 'standard/relative/blank path should be ignored'() {
        // given
        IOUtils.write(resource, 'main.go', '''
package main
import (
    "fmt"
    "go/types"
    "log"
    "os"
    "strings"
    "unicode"
    "unicode/utf8"
    "./another"
    `../another`
    _ "./another"
    A "../another"
    " "
    '\t\t'
    )
func main(){}
''')
        // when
        GolangDependencySet result = factory.produce(module).get()
        assert result.isEmpty()
    }

    @Test
    void 'import paths should be processed correctly'() {
        // given
        IOUtils.write(resource, 'main.go', mainDotGo)
        // when
        GolangDependencySet result = factory.produce(module).get()
        // then
        assert result.size() == 1
        assert result.any { it.is(dependency) }
    }

    @Test
    void 'all directory should be searched'() {
        // given
        File sub = resource.toPath().resolve('sub').toFile()
        File subsub = sub.toPath().resolve('sub').toFile()
        IOUtils.forceMkdir(sub)
        IOUtils.forceMkdir(sub)
        IOUtils.write(subsub, 'main.go', mainDotGo)
        IOUtils.write(subsub, 'garbage', 'This is unused')
        // when
        GolangDependencySet result = factory.produce(module).get()
        // then
        assert result.size() == 1
        assert result.any { it.is(dependency) }
    }

    String mainDotGo = '''
package main
import (
    "github.com/a/b"
    "github.com/a/b/c"
    "github.com/a/b/c/d"
)
func main(){}
'''


}
