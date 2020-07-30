package org.jetbrains.dokka.javadoc

import javadoc.JavadocDocumentableToPageTranslator
import javadoc.location.JavadocLocationProviderFactory
import javadoc.pages.AllClassesPageInstaller
import javadoc.pages.ResourcesInstaller
import javadoc.pages.TreeViewInstaller
import javadoc.renderer.KorteJavadocRenderer
import javadoc.signatures.JavadocSignatureProvider
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.PackageListCreator
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.kotlinAsJava.KotlinAsJavaPlugin
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.pages.PageTransformer

class JavadocPlugin : DokkaPlugin() {

    val dokkaBasePlugin by lazy { plugin<DokkaBase>() }
    val kotinAsJavaPlugin by lazy { plugin<KotlinAsJavaPlugin>() }
    val javadocPreprocessors by extensionPoint<PageTransformer>()

//    val locationProviderFactory by extensionPoint<JavadocLocationProviderFactory>()

    val dokkaJavadocPlugin by extending {
        (CoreExtensions.renderer
                providing { ctx -> KorteJavadocRenderer(dokkaBasePlugin.querySingle { outputWriter }, ctx, "views") }
                override dokkaBasePlugin.htmlRenderer)
    }

    val pageTranslator by extending {
        CoreExtensions.documentableToPageTranslator providing { context ->
            JavadocDocumentableToPageTranslator(
                dokkaBasePlugin.querySingle { commentsToContentConverter },
                dokkaBasePlugin.querySingle { signatureProvider },
                context.logger
            )
        } override dokkaBasePlugin.documentableToPageTranslator
    }

    val javadocLocationProviderFactory by extending {
        dokkaBasePlugin.locationProviderFactory providing { context ->
            JavadocLocationProviderFactory(context)
        } override dokkaBasePlugin.locationProvider
    }

    val javadocSignatureProvider by extending {
        val dokkaBasePlugin = plugin<DokkaBase>()
        dokkaBasePlugin.signatureProvider providing { ctx ->
            JavadocSignatureProvider(
                ctx.single(
                    dokkaBasePlugin.commentsToContentConverter
                ), ctx.logger
            )
        } override kotinAsJavaPlugin.javaSignatureProvider
    }

    val rootCreator by extending {
        javadocPreprocessors with RootCreator
    }

    val packageListCreator by extending {
        javadocPreprocessors providing {
            PackageListCreator(
                it,
                "dokkaJavadoc",
                "html"
            )
        } order { after(rootCreator) }
    }

    val resourcesInstaller by extending {
        javadocPreprocessors with ResourcesInstaller order { after(rootCreator) }
    }

    val treeViewInstaller by extending {
        javadocPreprocessors with TreeViewInstaller order { after(rootCreator) }
    }

    val allClassessPageInstaller by extending {
        javadocPreprocessors with AllClassesPageInstaller order { before(rootCreator) }
    }
}

