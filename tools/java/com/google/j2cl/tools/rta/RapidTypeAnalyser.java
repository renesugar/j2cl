/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.tools.rta;

import static com.google.common.base.Predicates.not;
import static java.util.stream.Collectors.toSet;

import com.google.j2cl.libraryinfo.LibraryInfo;
import java.util.List;
import java.util.Set;

final class RapidTypeAnalyser {

  static RtaResult analyse(List<LibraryInfo> libraryInfos) {
    List<Type> types = TypeGraphBuilder.build(libraryInfos);

    // Go over the entry points to start the traversal.
    types.stream()
        .flatMap(t -> t.getMembers().stream())
        .filter(Member::isJsAccessible)
        .forEach(m -> onMemberReference(m));

    return RtaResult.build(getUnusedTypes(types), getUnusedMembers(types));
  }

  private static Set<Type> getUnusedTypes(List<Type> types) {
    return types.stream().filter(not(Type::isLive)).collect(toSet());
  }

  private static Set<Member> getUnusedMembers(List<Type> types) {
    return types.stream()
        .filter(Type::isLive)
        .flatMap(t -> t.getMembers().stream())
        .filter(not(Member::isLive))
        .collect(toSet());
  }

  private static void onMemberReference(Member member) {
    if (member.isPolymorphic()) {
      traversePolymorphicReference(member.getDeclaringType(), member.getName());
    } else {
      onTypeReference(member.getDeclaringType());
      markMemberLive(member);
    }
  }

  private static void onTypeReference(Type type) {
    markTypeLive(type);
    markMemberLive(type.getMemberByName("$clinit"));
  }

  private static void markMemberLive(Member member) {
    if (member.isLive()) {
      return;
    }

    member.markLive();

    Type declaringType = member.getDeclaringType();
    if (!declaringType.isInstantiated() && member.isConstructor()) {
      declaringType.instantiate();
      declaringType.getPotentiallyLiveMembers().forEach(RapidTypeAnalyser::markMemberLive);
    }

    member.getReferencedTypes().forEach(RapidTypeAnalyser::onTypeReference);
    member.getReferencedMembers().forEach(RapidTypeAnalyser::onMemberReference);
  }

  private static void traversePolymorphicReference(Type type, String memberName) {
    Member member = type.getMemberByName(memberName);
    if (member != null) {
      if (member.isFullyTraversed()) {
        return;
      }
      member.markFullyTraversed();

      markMemberPotentiallyLive(member);
    }

    // Recursively unfold the overriding chain.
    type.getImmediateSubtypes()
        .forEach(subtype -> traversePolymorphicReference(subtype, memberName));

    markOverriddenMembersPotentiallyLive(type, memberName);
  }

  private static void markOverriddenMembersPotentiallyLive(Type type, String memberName) {
    while ((type = type.getSuperClass()) != null) {
      Member member = type.getMemberByName(memberName);
      if (member == null) {
        continue;
      }
      if (!member.isPolymorphic()) {
        return;
      }
      markMemberPotentiallyLive(member);
    }
  }

  private static void markMemberPotentiallyLive(Member member) {
    Type declaringType = member.getDeclaringType();
    if (declaringType.isInstantiated()) {
      markMemberLive(member);
    } else {
      // Type is not instantiated, defer making it live until the type is instantiated.
      declaringType.addPotentiallyLiveMember(member);
    }
  }

  private static void markTypeLive(Type type) {
    if (type.isLive()) {
      return;
    }

    type.markLive();

    // When a type is marked as live, we need to explicitly mark the super interfaces as live since
    // we need markImplementor call (which are not tracked in AST).
    type.getSuperInterfaces().forEach(RapidTypeAnalyser::markTypeLive);
  }

  private RapidTypeAnalyser() {}
}
