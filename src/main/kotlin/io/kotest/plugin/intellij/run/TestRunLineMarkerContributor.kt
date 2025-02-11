package io.kotest.plugin.intellij.run

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.Function
import io.kotest.plugin.intellij.Test
import io.kotest.plugin.intellij.psi.enclosingKtClass
import io.kotest.plugin.intellij.psi.specStyle
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclarationModifierList
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtPackageDirective

/**
 * Returns a [RunLineMarkerContributor.Info] if the element is a test case.
 */
class TestRunLineMarkerContributor : RunLineMarkerContributor() {

   // icons list https://jetbrains.design/intellij/resources/icons_list/
   private val icon = AllIcons.RunConfigurations.TestState.Run

   override fun getInfo(element: PsiElement): Info? {
      // the docs say to only run a line marker for a leaf
      return when (element) {
         is LeafPsiElement -> {
            when (element.context) {
               // rule out some common entries that can't possibly be test markers for performance
               is KtAnnotationEntry, is KtDeclarationModifierList, is KtImportDirective, is KtImportList, is KtPackageDirective -> null
               else -> markerIfTest(element)
            }
         }
         else -> null
      }
   }

   /**
    * Returns an [Info] if this element is a test that is enabled.
    * Disabled tests are handled by the [DisabledTestLineMarker].
    */
   private fun markerIfTest(element: LeafPsiElement): Info? {
      val ktclass = element.enclosingKtClass() ?: return null
      val style = ktclass.specStyle() ?: return null
      val test = style.test(element) ?: return null
      // we cannot run interpolated names via the plugin
      if (test.name.interpolated) return null
      // disabled tests are handled by another line marker
      if (!test.enabled) return null
      return icon(test)
   }

   /**
    * Returns an [Info] to use for the given [Test].
    */
   private fun icon(test: Test): Info {
      return Info(
         icon,
         Function<PsiElement, String> { "Run ${test.readableTestPath()}" },
         *ExecutorAction.getActions(1)
      )
   }
}
