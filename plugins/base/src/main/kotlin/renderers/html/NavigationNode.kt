package org.jetbrains.dokka.base.renderers.html

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.links.DRI

data class NavigationNode(
    val name: String,
    val dri: DRI,
    val sourceSets: Set<DokkaSourceSet>,
    val children: List<NavigationNode>
)
