package io.kotest.plugin.intellij

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.Function
import io.kotest.plugin.intellij.psi.enclosingClass
import io.kotest.plugin.intellij.psi.enclosingClassOrObjectForClassOrObjectToken
import io.kotest.plugin.intellij.psi.isSpecSubclass
import io.kotest.plugin.intellij.psi.isSubclassOfSpec
import io.kotest.plugin.intellij.styles.SpecStyle
import io.kotest.plugin.intellij.styles.Test
import org.jetbrains.kotlin.psi.KtClassOrObject

/**
 * Given an element, returns an [RunLineMarkerContributor.Info] if the elements line should have a gutter icon added.
 */
class KotestRunLineMarkerContributor : RunLineMarkerContributor() {

   override fun getInfo(element: PsiElement): Info? {
      // the docs say to only run a line marker for a leaf
      return when (element) {
         is LeafPsiElement -> markerForSpec(element) ?: markerForTest(element)
         else -> null
      }
   }

   private fun markerForSpec(element: LeafPsiElement): Info? {
      val ktclass = element.enclosingClassOrObjectForClassOrObjectToken() ?: return null
      return SpecStyle.styles.asSequence()
         .filter { ktclass.isSpecSubclass(it) }
         .map { icon(ktclass) }
         .firstOrNull()
   }

   private fun markerForTest(element: LeafPsiElement): Info? {

      // must be included in a spec class, pulled this check outside of the main sequence to avoid
      // grabbing the class parents over and over
      val ktclass = element.enclosingClass() ?: return null
      if (!SpecStyle.styles.any { ktclass.isSubclassOfSpec() }) return null

      return SpecStyle.styles.asSequence()
         .map { it.test(element) }
         .filterNotNull()
         .map { icon(it) }
         .firstOrNull()
   }

   private fun icon(ktclass: KtClassOrObject): Info {
      return Info(
         AllIcons.RunConfigurations.TestState.Run_run,
         Function<PsiElement, String> { "Run ${ktclass.fqName!!.shortName()}" },
         *ExecutorAction.getActions(1)
      )
   }

   private fun icon(test: Test): Info {
      return Info(
         AllIcons.RunConfigurations.TestState.Run,
         Function<PsiElement, String> { "Run ${test.path}" },
         *ExecutorAction.getActions(1)
      )
   }
}
