using ArchUnitNET.Domain;
using ArchUnitNET.Loader;
using Xunit;

namespace Lmp.ArchitectureTests;

/// <summary>
/// Spring-Modulith-style architectural guardrails for the LMP modular monolith.
/// Built on <b>ArchUnitNET</b> — the .NET port of ArchUnit, the very engine Spring
/// Modulith delegates its verification to — used here as the IL dependency-graph
/// loader/model, with every rule asserted in plain C# against that graph (no fragile
/// fluent-DSL coupling). The rules enforce:
/// <list type="bullet">
///   <item>the Clean Architecture layering (dependency direction);</item>
///   <item>module encapsulation — a module's internals are private; cross-module
///         collaboration goes only through the <c>Lmp.Application.&lt;Module&gt;</c>
///         abstractions (the module's "named interface");</item>
///   <item>an acyclic module graph (rejected, exactly like Spring Modulith).</item>
/// </list>
/// Any violation fails the build — the architectural ratchet. Boundary/cycle checks
/// are computed from the real IL graph, and <see cref="Rules_actually_have_types_to_evaluate"/>
/// makes a rule that matched nothing fail loudly (our hedge against the ArchUnit
/// "unreadable bytecode silently passes" caveat learned on the Java side).
/// </summary>
public sealed class ArchitectureTests
{
    private static readonly Architecture Architecture = new ArchLoader()
        .LoadAssemblies(
            typeof(Lmp.Domain.Auth.User).Assembly,
            typeof(Lmp.Application.Auth.IAuthService).Assembly,
            typeof(Lmp.Infrastructure.DependencyInjection).Assembly,
            typeof(Lmp.Api.Controllers.Auth.AuthController).Assembly)
        .Build();

    /// <summary>The vertical slices (business modules) of the monolith.</summary>
    private static readonly string[] Modules =
    {
        "Auth", "Billing", "Catalog", "Crm", "Notification", "Portal",
        "Pricing", "SiteConfiguration", "Admin", "Seo", "RealTime",
    };

    // ---------------- Clean Architecture layering ----------------

    [Fact]
    public void Domain_depends_on_no_other_layer() => AssertNoEdge(
        (o, t) => LayerOf(o) == "Domain" && LayerOf(t) is "Application" or "Infrastructure" or "Api",
        "Le Domaine ne doit dépendre d'aucune couche externe (règle Clean Architecture).");

    [Fact]
    public void Application_does_not_depend_on_infrastructure_or_api() => AssertNoEdge(
        (o, t) => LayerOf(o) == "Application" && LayerOf(t) is "Infrastructure" or "Api",
        "L'Application ne doit dépendre ni de l'Infrastructure ni de l'API (inversion de dépendance).");

    [Fact]
    public void Infrastructure_does_not_depend_on_api() => AssertNoEdge(
        (o, t) => LayerOf(o) == "Infrastructure" && LayerOf(t) == "Api",
        "L'Infrastructure ne doit pas dépendre de la couche API.");

    [Fact]
    public void Controllers_depend_only_on_application_not_infrastructure() => AssertNoEdge(
        (o, t) => o.StartsWith("Lmp.Api.Controllers", StringComparison.Ordinal) && LayerOf(t) == "Infrastructure",
        "Les contrôleurs doivent passer par les interfaces Application, jamais par l'Infrastructure.");

    [Fact]
    public void Application_is_persistence_agnostic()
    {
        var violations = AllEdges()
            .Where(e => LayerOf(Ns(e.origin)) == "Application"
                        && (Ns(e.target).StartsWith("Microsoft.EntityFrameworkCore", StringComparison.Ordinal)
                            || Ns(e.target).StartsWith("Npgsql", StringComparison.Ordinal)))
            .Select(Describe).Distinct().OrderBy(s => s).ToList();

        Assert.True(violations.Count == 0,
            "L'Application doit rester agnostique de la persistance (pas d'EF Core / Npgsql) :\n  "
            + string.Join("\n  ", violations));
    }

    // ---------------- Structural conventions ----------------

    [Fact]
    public void Controllers_live_only_in_the_api_layer()
    {
        var misplaced = Architecture.Types
            .Where(t => Ns(t).StartsWith("Lmp.", StringComparison.Ordinal)
                        && t.Name.EndsWith("Controller", StringComparison.Ordinal)
                        && !Ns(t).StartsWith("Lmp.Api", StringComparison.Ordinal))
            .Select(t => t.FullName).Distinct().OrderBy(s => s).ToList();

        Assert.True(misplaced.Count == 0, "Des contrôleurs vivent hors de la couche API :\n  " + string.Join("\n  ", misplaced));
    }

    [Fact]
    public void DbContext_lives_only_in_persistence()
    {
        var misplaced = Architecture.Types
            .Where(t => Ns(t).StartsWith("Lmp.", StringComparison.Ordinal)
                        && t.Name.EndsWith("DbContext", StringComparison.Ordinal)
                        && !Ns(t).StartsWith("Lmp.Infrastructure.Persistence", StringComparison.Ordinal))
            .Select(t => t.FullName).Distinct().OrderBy(s => s).ToList();

        Assert.True(misplaced.Count == 0, "Un DbContext vit hors de Lmp.Infrastructure.Persistence :\n  " + string.Join("\n  ", misplaced));
    }

    // ---------------- Module boundaries (from the real IL graph) ----------------

    /// <summary>
    /// A module's implementation (<c>Lmp.Infrastructure.&lt;Module&gt;</c>) is internal:
    /// only the module itself and the composition root (<c>Lmp.Infrastructure</c>) may
    /// reference it. Everyone else must go through the <c>Lmp.Application.&lt;Module&gt;</c>
    /// interfaces — the Spring-Modulith "internal types are off-limits" rule.
    /// </summary>
    [Fact]
    public void Module_internals_are_not_accessed_across_modules()
    {
        var violations = LmpEdges()
            .Where(e =>
            {
                var targetModule = InfraModuleOf(Ns(e.target));
                if (targetModule is null) return false;              // target is not a module's internals
                var originNs = Ns(e.origin);
                if (originNs == "Lmp.Infrastructure") return false;   // composition root may wire everything
                return ModuleOf(originNs) != targetModule;            // foreign module reaching in => violation
            })
            .Select(Describe).Distinct().OrderBy(s => s).ToList();

        Assert.True(violations.Count == 0,
            "Un module accède à l'Infrastructure interne d'un autre module — passez par les interfaces "
            + "Lmp.Application.<Module> :\n  " + string.Join("\n  ", violations));
    }

    /// <summary>Modules must form a DAG — cyclic dependencies are rejected, like Spring Modulith.</summary>
    [Fact]
    public void Module_graph_is_acyclic()
    {
        var edges = new Dictionary<string, HashSet<string>>();
        foreach (var (origin, target) in LmpEdges())
        {
            var a = ModuleOf(Ns(origin));
            var b = ModuleOf(Ns(target));
            if (a is null || b is null || a == b) continue;
            if (!edges.TryGetValue(a, out var set)) edges[a] = set = new HashSet<string>();
            set.Add(b);
        }

        var cycle = FindCycle(edges);
        Assert.True(cycle is null, "Cycle de dépendances entre modules (interdit) : " + cycle);
    }

    // ---------------- Guard the guards (ArchUnit bytecode caveat) ----------------

    [Fact]
    public void Rules_actually_have_types_to_evaluate()
    {
        var lmpTypes = Architecture.Types.Count(t => Ns(t).StartsWith("Lmp.", StringComparison.Ordinal));
        Assert.True(lmpTypes > 100,
            $"Seulement {lmpTypes} types Lmp chargés — les règles risquent de passer à vide (caveat bytecode/namespace).");

        var detected = Architecture.Types.Select(t => ModuleOf(Ns(t))).Where(m => m is not null).Distinct().ToHashSet();
        var missing = Modules.Where(m => !detected.Contains(m)).ToList();
        Assert.True(missing.Count == 0, "Modules attendus non détectés (namespace renommé ?) : " + string.Join(", ", missing));
    }

    // ---------------- helpers ----------------

    private static void AssertNoEdge(Func<string, string, bool> isViolation, string because)
    {
        var violations = LmpEdges()
            .Where(e => isViolation(Ns(e.origin), Ns(e.target)))
            .Select(Describe).Distinct().OrderBy(s => s).ToList();

        Assert.True(violations.Count == 0, because + "\n  " + string.Join("\n  ", violations));
    }

    private static string Describe((IType origin, IType target) e) => $"{e.origin.FullName}  ->  {e.target.FullName}";

    private static string Ns(IType t) => t.Namespace?.FullName ?? string.Empty;

    private static string? LayerOf(string ns)
    {
        if (!ns.StartsWith("Lmp.", StringComparison.Ordinal)) return null;
        var p = ns.Split('.');
        return p.Length >= 2 && p[1] is "Domain" or "Application" or "Infrastructure" or "Api" ? p[1] : null;
    }

    /// <summary>Maps a namespace to its business module, or null for shared/non-module code.</summary>
    private static string? ModuleOf(string ns)
    {
        if (!ns.StartsWith("Lmp.", StringComparison.Ordinal)) return null;
        var p = ns.Split('.');
        if (p.Length < 3) return null;                                   // "Lmp", "Lmp.Infrastructure" (root)
        var seg = (p[1] == "Api" && p[2] == "Controllers" && p.Length >= 4) ? p[3] : p[2];
        return Array.IndexOf(Modules, seg) >= 0 ? seg : null;            // null for Common/Persistence/Content/...
    }

    /// <summary>Returns the module whose Infrastructure internals this namespace belongs to, else null.</summary>
    private static string? InfraModuleOf(string ns)
    {
        if (!ns.StartsWith("Lmp.Infrastructure.", StringComparison.Ordinal)) return null;
        var seg = ns.Split('.')[2];
        return Array.IndexOf(Modules, seg) >= 0 ? seg : null;            // excludes Persistence
    }

    private static IEnumerable<(IType origin, IType target)> AllEdges()
    {
        foreach (var t in Architecture.Types)
        {
            if (!Ns(t).StartsWith("Lmp.", StringComparison.Ordinal)) continue;
            foreach (var d in t.Dependencies)
                if (d.Target is { } target)
                    yield return (t, target);
        }
    }

    private static IEnumerable<(IType origin, IType target)> LmpEdges() =>
        AllEdges().Where(e => Ns(e.target).StartsWith("Lmp.", StringComparison.Ordinal));

    private static string? FindCycle(Dictionary<string, HashSet<string>> edges)
    {
        var state = new Dictionary<string, int>();   // 0/absent = unvisited, 1 = on stack, 2 = done
        var stack = new List<string>();

        string? Visit(string node)
        {
            state[node] = 1;
            stack.Add(node);
            if (edges.TryGetValue(node, out var nexts))
            {
                foreach (var n in nexts)
                {
                    state.TryGetValue(n, out var s);
                    if (s == 0)
                    {
                        var r = Visit(n);
                        if (r is not null) return r;
                    }
                    else if (s == 1)
                    {
                        return string.Join(" -> ", stack.Skip(stack.IndexOf(n))) + " -> " + n;
                    }
                }
            }
            stack.RemoveAt(stack.Count - 1);
            state[node] = 2;
            return null;
        }

        foreach (var node in edges.Keys)
        {
            state.TryGetValue(node, out var s);
            if (s == 0)
            {
                var r = Visit(node);
                if (r is not null) return r;
            }
        }
        return null;
    }
}
